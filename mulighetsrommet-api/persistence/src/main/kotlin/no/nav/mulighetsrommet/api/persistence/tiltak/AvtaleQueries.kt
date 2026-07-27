package no.nav.mulighetsrommet.api.persistence.tiltak

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.opplaring.toOpplaringKategorisering
import no.nav.mulighetsrommet.admin.tiltak.AvtaleDto
import no.nav.mulighetsrommet.admin.tiltak.AvtaleQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.toPrismodellDto
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleRepository
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.persistence.opplaring.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.utils.parameters
import no.nav.mulighetsrommet.database.utils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleQueries(private val session: Session) : AvtaleRepository, AvtaleQueryHandler {
    override fun save(avtale: Avtale) = withTransaction(session) {
        upsertDetaljer(avtale.id, avtale)
        upsertAdministratorer(avtale.id, avtale.administratorer)
        upsertArrangor(avtale.id, avtale.arrangor)
        OpplaringKategoriseringQueries(session).upsert(avtale.id, avtale.opplaring)
        upsertRedaksjoneltInnhold(avtale.id, avtale.veilederinfo.beskrivelse, avtale.veilederinfo.faneinnhold)
        upsertNavEnheter(avtale.id, avtale.veilederinfo.navEnheter)
        updatePersonvern(avtale.id, avtale.personvern)
        syncPrismodeller(avtale.id, avtale.prisinfo)
    }

    private fun syncPrismodeller(avtaleId: UUID, prisinfo: Avtale.Prisinfo) {
        when (prisinfo) {
            is Avtale.Prisinfo.Systembestemt -> upsertPrismodell(avtaleId, prisinfo.prismodell.id)
            is Avtale.Prisinfo.Egendefinert -> syncEgendefinertePrismodeller(avtaleId, prisinfo.prismodeller)
        }
    }

    private fun syncEgendefinertePrismodeller(avtaleId: UUID, prismodeller: List<Prismodell>) {
        val desiredIds = prismodeller.map { it.id }.toSet()

        @Language("PostgreSQL")
        val currentIdsQuery = "select prismodell_id from avtale_prismodell where avtale_id = ?::uuid"
        val currentIds = session.list(queryOf(currentIdsQuery, avtaleId)) { it.uuid("prismodell_id") }.toSet()

        val prismodellQueries = PrismodellQueries(session)
        prismodeller.forEach { prismodell ->
            prismodellQueries.upsert(prismodell)
            upsertPrismodell(avtaleId, prismodell.id)
        }

        (currentIds - desiredIds).forEach { removedId ->
            deletePrismodell(avtaleId, removedId)
            prismodellQueries.deletePrismodell(removedId)
        }
    }

    fun updatePersonvern(
        avtaleId: UUID,
        personvern: Avtale.Personvern,
    ) = withTransaction(session) {
        @Language("PostgreSQL")
        val updatePersonvernBekreftet = """
            update avtale
            set personvern_bekreftet = :personvern_bekreftet::boolean
            where id = :id::uuid
        """.trimIndent()
        execute(
            queryOf(
                updatePersonvernBekreftet,
                mapOf("id" to avtaleId, "personvern_bekreftet" to personvern.erBekreftet),
            ),
        )

        val nonAnnetTypes = personvern.personopplysninger.filter { it != Personopplysning.Type.ANNET }

        @Language("PostgreSQL")
        val updatePersonopplysninger = """
            insert into avtale_personopplysning (avtale_id, personopplysning)
            values (?::uuid, ?)
            on conflict do nothing
        """.trimIndent()
        batchPreparedStatement(
            updatePersonopplysninger,
            nonAnnetTypes.map { listOf(avtaleId, it.name) },
        )

        @Language("PostgreSQL")
        val updatePersonopplysningAnnet = """
            insert into avtale_personopplysning (avtale_id, personopplysning, beskrivelse)
            values (?::uuid, ?, ?)
            on conflict (avtale_id, personopplysning) do update set beskrivelse = excluded.beskrivelse
        """.trimIndent()
        if (Personopplysning.Type.ANNET in personvern.personopplysninger) {
            execute(
                queryOf(
                    updatePersonopplysningAnnet,
                    avtaleId,
                    Personopplysning.Type.ANNET.name,
                    personvern.annetBeskrivelse,
                ),
            )
        }

        @Language("PostgreSQL")
        val deletePersonopplysninger = """
            delete from avtale_personopplysning
            where avtale_id = ?::uuid and not (personopplysning = any (?))
        """.trimIndent()
        execute(
            queryOf(
                deletePersonopplysninger,
                avtaleId,
                createTextArray(personvern.personopplysninger.map { it.name }),
            ),
        )
    }

    private fun upsertAdministratorer(avtaleId: UUID, administratorer: Set<NavIdent>) = withTransaction(session) {
        @Language("PostgreSQL")
        val upsertAdministrator = """
             insert into avtale_administrator(avtale_id, nav_ident)
             values (?::uuid, ?)
             on conflict (avtale_id, nav_ident) do nothing
        """.trimIndent()
        batchPreparedStatement(upsertAdministrator, administratorer.map { listOf(avtaleId, it.value) })

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from avtale_administrator
             where avtale_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()
        execute(
            queryOf(
                deleteAdministratorer,
                avtaleId,
                createArrayOfValue(administratorer) { it.value },
            ),
        )
    }

    private fun upsertArrangor(avtaleId: UUID, arrangor: Avtale.Arrangor?) = withTransaction(session) {
        @Language("PostgreSQL")
        val setArrangorUnderenhet = """
             insert into avtale_arrangor_underenhet (avtale_id, arrangor_id)
             values (?::uuid, ?::uuid)
             on conflict (avtale_id, arrangor_id) do nothing
        """.trimIndent()
        arrangor?.underenheter?.also { underenheter ->
            batchPreparedStatement(setArrangorUnderenhet, underenheter.map { listOf(avtaleId, it) })
        }

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from avtale_arrangor_underenhet
             where avtale_id = ?::uuid and not (arrangor_id = any (?))
        """.trimIndent()
        execute(
            queryOf(
                deleteUnderenheter,
                avtaleId,
                createUuidArray(arrangor?.underenheter.orEmpty()),
            ),
        )

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into avtale_arrangor_kontaktperson (avtale_id, arrangor_kontaktperson_id)
            values (?::uuid, ?::uuid)
            on conflict do nothing
        """.trimIndent()
        arrangor?.kontaktpersoner?.also { kontaktpersoner ->
            batchPreparedStatement(upsertArrangorKontaktperson, kontaktpersoner.map { listOf(avtaleId, it) })
        }

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from avtale_arrangor_kontaktperson
            where avtale_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()
        execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                avtaleId,
                createUuidArray(arrangor?.kontaktpersoner.orEmpty()),
            ),
        )

        @Language("PostgreSQL")
        val updateAvtaleArrangor = """
            update avtale
            set arrangor_hovedenhet_id = :arrangor_hovedenhet_id
            where id = :id::uuid
        """.trimIndent()
        execute(
            queryOf(
                updateAvtaleArrangor,
                mapOf(
                    "id" to avtaleId,
                    "arrangor_hovedenhet_id" to arrangor?.hovedenhet,
                ),
            ),
        )
    }

    private fun upsertDetaljer(id: UUID, avtale: Avtale) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale (
                id,
                navn,
                tiltakstype_id,
                sakarkiv_nummer,
                arrangor_hovedenhet_id,
                start_dato,
                slutt_dato,
                status,
                avtaletype,
                opsjon_maks_varighet,
                opsjonsmodell,
                opsjon_custom_opsjonsmodell_navn
            ) values (
                :id::uuid,
                :navn,
                (select id from tiltakstype where tiltakskode = :tiltakskode),
                :sakarkiv_nummer,
                :arrangor_hovedenhet_id,
                :start_dato,
                :slutt_dato,
                :status::avtale_status,
                :avtaletype::avtaletype,
                :opsjon_maks_varighet,
                :opsjonsmodell::opsjonsmodell,
                :opsjon_custom_opsjonsmodell_navn
            )
            on conflict (id) do update set
                navn = excluded.navn,
                sakarkiv_nummer = excluded.sakarkiv_nummer,
                arrangor_hovedenhet_id = excluded.arrangor_hovedenhet_id,
                start_dato = excluded.start_dato,
                slutt_dato = excluded.slutt_dato,
                status = excluded.status,
                avtaletype = excluded.avtaletype,
                opsjon_maks_varighet = excluded.opsjon_maks_varighet,
                opsjonsmodell = excluded.opsjonsmodell,
                opsjon_custom_opsjonsmodell_navn = excluded.opsjon_custom_opsjonsmodell_navn
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "navn" to avtale.navn,
            "tiltakskode" to avtale.tiltakskode.name,
            "sakarkiv_nummer" to avtale.sakarkivNummer?.value,
            "arrangor_hovedenhet_id" to avtale.arrangor?.hovedenhet,
            "start_dato" to avtale.startDato,
            "slutt_dato" to avtale.sluttDato,
            "avtaletype" to avtale.avtaletype.name,
            "status" to avtale.status.type.name,
            "opsjonsmodell" to avtale.opsjoner.modell.type.name,
            "opsjon_maks_varighet" to avtale.opsjoner.modell.opsjonMaksVarighet,
            "opsjon_custom_opsjonsmodell_navn" to avtale.opsjoner.modell.customOpsjonsmodellNavn,
        )
        execute(queryOf(query, params))
    }

    private fun upsertRedaksjoneltInnhold(
        id: UUID,
        beskrivelse: String?,
        faneinnhold: Faneinnhold?,
    ) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set beskrivelse = :beskrivelse,
                faneinnhold = :faneinnhold::jsonb
            where id = :id::uuid
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "beskrivelse" to beskrivelse,
            "faneinnhold" to Json.encodeToString<Faneinnhold?>(faneinnhold),
        )
        session.execute(queryOf(query, params))
    }

    private fun upsertNavEnheter(avtaleId: UUID, enheter: Set<NavEnhetNummer>) = withTransaction(session) {
        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into avtale_nav_enhet (avtale_id, enhetsnummer)
             values (?::uuid, ?)
             on conflict (avtale_id, enhetsnummer) do nothing
        """.trimIndent()
        batchPreparedStatement(upsertEnhet, enheter.map { listOf(avtaleId, it.value) })

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from avtale_nav_enhet
             where avtale_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()
        execute(queryOf(deleteEnheter, avtaleId, createArrayOfValue(enheter) { it.value }))
    }

    private fun upsertPrismodell(avtaleId: UUID, prismodellId: UUID) {
        @Language("PostgreSQL")
        val updateAvtalenummer = """
            insert into avtale_prismodell (avtale_id, prismodell_id)
            values (:avtale_id, :prismodell_id)
            on conflict (avtale_id, prismodell_id) do nothing
        """.trimIndent()
        val params = mapOf("avtale_id" to avtaleId, "prismodell_id" to prismodellId)
        session.execute(queryOf(updateAvtalenummer, params))
    }

    private fun deletePrismodell(avtaleId: UUID, prismodellId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale_prismodell
            where avtale_id = ?::uuid and prismodell_id = ?::uuid
        """.trimIndent()
        session.execute(queryOf(query, avtaleId, prismodellId))
    }

    fun upsertAvtalenummer(id: UUID, avtalenummer: String) {
        @Language("PostgreSQL")
        val updateAvtalenummer = """
                update avtale
                set avtalenummer = :avtalenummer
                where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "avtalenummer" to avtalenummer)
        session.execute(queryOf(updateAvtalenummer, params))
    }

    override fun getOrError(id: UUID): Avtale = checkNotNull(get(id)) { "Avtale med id=$id mangler" }

    override fun get(id: UUID): Avtale? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_avtale
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toAvtale() }
    }

    override fun getAvtaleDto(id: UUID): AvtaleDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_avtale
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toAvtaleDto() }
    }

    override fun getAllAvtaleDto(
        pagination: Pagination,
        tiltakstyper: List<UUID>,
        search: String?,
        statuser: List<AvtaleStatusType>,
        avtaletyper: List<Avtaletype>,
        navEnheter: List<NavEnhetNummer>,
        sortering: String?,
        arrangorIds: List<UUID>,
        administratorNavIdent: NavIdent?,
        personvernBekreftet: Boolean?,
    ): PaginatedResult<AvtaleDto> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "tiltakstype_ids" to tiltakstyper.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { createUuidArray(it) },
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "avtaletyper" to avtaletyper.ifEmpty { null }?.let { createArrayOfAvtaletype(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfAvtaleStatus(statuser) },
            "personvern_bekreftet" to personvernBekreftet,
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "arrangor-ascending" -> "arrangor_hovedenhet_navn asc"
            "arrangor-descending" -> "arrangor_hovedenhet_navn desc"
            "startdato-ascending" -> "start_dato asc, navn asc"
            "startdato-descending" -> "start_dato desc, navn asc"
            "sluttdato-ascending" -> "slutt_dato asc, navn asc"
            "sluttdato-descending" -> "slutt_dato desc, navn asc"
            "tiltakstype_navn-ascending" -> "tiltakstype_navn asc, navn asc"
            "tiltakstype_navn-descending" -> "tiltakstype_navn desc, navn desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_avtale
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any (:tiltakstype_ids))
              and (:search::text is null or (fts @@ to_tsquery('norwegian', :search) or arrangor_hovedenhet_navn ilike :search_arrangor))
              and (:nav_enheter::text[] is null or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)))
              and (:arrangor_ids::text[] is null or arrangor_hovedenhet_id = any (:arrangor_ids))
              and (:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb)
              and (:avtaletyper::avtaletype[] is null or avtaletype = any (:avtaletyper))
              and (:statuser::text[] is null or status = any(:statuser))
              and (:personvern_bekreftet::boolean is null or personvern_bekreftet = :personvern_bekreftet::boolean)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toAvtaleDto() }
            .runWithSession(this)
    }

    fun setStatus(
        id: UUID,
        status: AvtaleStatusType,
        tidspunkt: LocalDateTime?,
        aarsaker: List<AvbrytAvtaleAarsak>?,
        forklaring: String?,
    ): Int {
        @Language("PostgreSQL")
        val query = """
            update avtale set
                status = :status::avtale_status,
                avbrutt_tidspunkt = :tidspunkt,
                avbrutt_aarsaker = :aarsaker,
                avbrutt_forklaring = :forklaring
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
            "tidspunkt" to tidspunkt,
            "aarsaker" to aarsaker?.let { session.createTextArray(it) },
            "forklaring" to forklaring,
        )
        return session.update(queryOf(query, params))
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and avtale_id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, kontaktpersonId, avtaleId))
    }

    fun setSluttDato(avtaleId: UUID, sluttDato: LocalDate) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set slutt_dato = ?
            where id = ?::uuid
        """

        session.update(queryOf(query, sluttDato, avtaleId))
    }

    override fun getPersonopplysninger(): List<Personopplysning> {
        @Language("PostgreSQL")
        val query = """ select * from personopplysning """

        return session.list(queryOf(query)) {
            Personopplysning(
                type = Personopplysning.Type.valueOf(it.string("value")),
                title = it.string("title"),
                helpText = it.stringOrNull("help_text"),
                sortKey = it.int("sort_key"),
            )
        }
    }
}

private fun Row.toAvtaleDto(): AvtaleDto {
    val arrangor = uuidOrNull("arrangor_hovedenhet_id")?.let { id ->
        val underenheter = stringOrNull("arrangor_underenheter_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorUnderenhet>>(it) }
            ?: emptyList()
        val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorKontaktperson>>(it) } ?: emptyList()
        AvtaleDto.ArrangorHovedenhet(
            id = id,
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
            navn = string("arrangor_hovedenhet_navn"),
            slettet = boolean("arrangor_hovedenhet_slettet"),
            underenheter = underenheter,
            kontaktpersoner = arrangorKontaktpersoner,
        )
    }

    val status = run {
        val status = AvtaleStatusType.valueOf(string("status"))
        val variant = when (status) {
            AvtaleStatusType.UTKAST, AvtaleStatusType.AVSLUTTET -> DataElement.Status.Variant.NEUTRAL
            AvtaleStatusType.AKTIV -> DataElement.Status.Variant.SUCCESS
            AvtaleStatusType.AVBRUTT -> DataElement.Status.Variant.ERROR
        }
        val description = stringOrNull("avbrutt_forklaring")
        AvtaleDto.Status(
            type = status,
            status = DataElement.Status(status.beskrivelse, variant, description),
        )
    }

    return AvtaleDto(
        id = uuid("id"),
        tiltakstype = AvtaleDto.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        navn = string("navn"),
        avtalenummer = stringOrNull("avtalenummer"),
        sakarkivNummer = stringOrNull("sakarkiv_nummer")?.let { SakarkivNummer(it) },
        arrangor = arrangor,
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        avtaletype = Avtaletype.valueOf(string("avtaletype")),
        status = status,
        administratorer = toAdministratorer(),
        kontorstruktur = Kontorstruktur.fromNavEnheter(toNavEnheter()),
        beskrivelse = stringOrNull("beskrivelse"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        personopplysninger = toPersonopplysninger(),
        personvernBekreftet = boolean("personvern_bekreftet"),
        opplaring = toOpplaringKategoriseringDetaljer(),
        opsjonsmodell = toOpsjonsmodell(),
        opsjonerRegistrert = toOpsjonerRegistrert(),
        prismodeller = toPrismodeller().map { it.toPrismodellDto() },
    )
}

private fun Row.toAvtale(): Avtale {
    val arrangor = uuidOrNull("arrangor_hovedenhet_id")?.let { id ->
        val underenheter = stringOrNull("arrangor_underenheter_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorUnderenhet>>(it) }
            ?.map { it.id }
            .orEmpty()
        val kontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorKontaktperson>>(it) }
            ?.map { it.id }
            .orEmpty()
        Avtale.Arrangor(
            hovedenhet = id,
            underenheter = underenheter,
            kontaktpersoner = kontaktpersoner,
        )
    }

    val status = when (AvtaleStatusType.valueOf(string("status"))) {
        AvtaleStatusType.AKTIV -> AvtaleStatus.Aktiv

        AvtaleStatusType.AVSLUTTET -> AvtaleStatus.Avsluttet

        AvtaleStatusType.UTKAST -> AvtaleStatus.Utkast

        AvtaleStatusType.AVBRUTT -> {
            AvtaleStatus.Avbrutt(
                localDateTime("avbrutt_tidspunkt"),
                arrayOrNull<String>("avbrutt_aarsaker")?.map { AvbrytAvtaleAarsak.valueOf(it) } ?: emptyList(),
                stringOrNull("avbrutt_forklaring"),
            )
        }
    }

    val avtaletype = Avtaletype.valueOf(string("avtaletype"))

    return Avtale(
        id = uuid("id"),
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        navn = string("navn"),
        avtalenummer = stringOrNull("avtalenummer"),
        sakarkivNummer = stringOrNull("sakarkiv_nummer")?.let { SakarkivNummer(it) },
        arrangor = arrangor,
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        avtaletype = avtaletype,
        status = status,
        administratorer = toAdministratorer().map { it.navIdent }.toSet(),
        veilederinfo = Avtale.VeilederInfo(
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            navEnheter = toNavEnheter().map { it.enhetsnummer }.toSet(),
        ),
        personvern = run {
            val personopplysninger = toPersonopplysninger()
            Avtale.Personvern(
                personopplysninger = personopplysninger.map { it.type }.toSet(),
                annetBeskrivelse = personopplysninger.find { it.type == Personopplysning.Type.ANNET }?.beskrivelse,
                erBekreftet = boolean("personvern_bekreftet"),
            )
        },
        opplaring = toOpplaringKategoriseringDetaljer()?.toOpplaringKategorisering(),
        opsjoner = Avtale.Opsjoner(
            modell = toOpsjonsmodell(),
            registreringer = toOpsjonerRegistrert(),
        ),
        prisinfo = when (avtaletype) {
            Avtaletype.FORHANDSGODKJENT -> Avtale.Prisinfo.Systembestemt(toPrismodeller().single())
            else -> Avtale.Prisinfo.Egendefinert(toPrismodeller())
        },
    )
}

private fun Row.toNavEnheter(): List<NavEnhetDto> = stringOrNull("nav_enheter_json")
    ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
    ?: emptyList()

private fun Row.toAdministratorer(): List<AvtaleDto.Administrator> = stringOrNull("administratorer_json")
    ?.let { Json.decodeFromString<List<AvtaleDto.Administrator>>(it) }
    ?: emptyList()

private fun Row.toPersonopplysninger(): List<Personopplysning> = stringOrNull("personopplysninger_json")
    ?.let { Json.decodeFromString<List<Personopplysning>>(it) }
    ?: emptyList()

private fun Row.toOpsjonsmodell(): Opsjonsmodell = Opsjonsmodell(
    type = OpsjonsmodellType.valueOf(string("opsjonsmodell")),
    opsjonMaksVarighet = localDateOrNull("opsjon_maks_varighet"),
    customOpsjonsmodellNavn = stringOrNull("opsjon_custom_opsjonsmodell_navn"),
)

private fun Row.toOpplaringKategoriseringDetaljer(): OpplaringKategoriseringDetaljer? {
    return stringOrNull("opplaring_kategorisering_json")?.let {
        Json.decodeFromString<OpplaringKategoriseringDetaljer>(it)
    }
}

private fun Row.toOpsjonerRegistrert(): List<Avtale.OpsjonLogg> = stringOrNull("opsjon_logg_json")
    ?.let { Json.decodeFromString<List<Avtale.OpsjonLogg>>(it) }
    .orEmpty()

val JsonIgnoreUnknownKeys = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun Row.toPrismodeller(): List<Prismodell> = JsonIgnoreUnknownKeys.decodeFromString<List<PrismodellRow>>(string("prismodeller_json")).map { row ->
    Prismodell.from(
        row.id,
        row.type,
        row.valuta,
        row.prisbetingelser,
        row.satser,
        row.tilsagnPerDeltaker,
        row.totalbelop,
        row.tilskudd,
        row.aarsak,
    )
}

@Serializable
private data class PrismodellRow(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: PrismodellType,
    val valuta: Valuta,
    val prisbetingelser: String?,
    val satser: List<AvtaltSats>?,
    val tilsagnPerDeltaker: Boolean?,
    val totalbelop: Int?,
    val tilskudd: Map<Opplaeringtilskudd.Kode, Int>?,
    val aarsak: String?,
)

fun Session.createArrayOfAvtaleStatus(
    avtaletypes: List<AvtaleStatusType>,
): Array = createArrayOf("avtale_status", avtaletypes)

fun Session.createArrayOfAvtaletype(
    avtaletypes: List<Avtaletype>,
): Array = createArrayOf("avtaletype", avtaletypes)
