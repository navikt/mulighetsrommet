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
    fun create(avtale: AvtaleDbo) = withTransaction(session) {
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
                :opsjonCustomOpsjonsmodellNavn
            )
        """.trimIndent()

        val params = mapOf(
            "id" to avtale.id,
            "navn" to avtale.detaljerDbo.navn,
            "tiltakskode" to avtale.detaljerDbo.tiltakskode.name,
            "sakarkiv_nummer" to avtale.detaljerDbo.sakarkivNummer?.value,
            "arrangor_hovedenhet_id" to avtale.detaljerDbo.arrangor?.hovedenhet,
            "start_dato" to avtale.detaljerDbo.startDato,
            "slutt_dato" to avtale.detaljerDbo.sluttDato,
            "status" to avtale.detaljerDbo.status.name,
            "avtaletype" to avtale.detaljerDbo.avtaletype.name,
            "opsjon_maks_varighet" to avtale.detaljerDbo.opsjonsmodell.opsjonMaksVarighet,
            "opsjonsmodell" to avtale.detaljerDbo.opsjonsmodell.type.name,
            "opsjonCustomOpsjonsmodellNavn" to avtale.detaljerDbo.opsjonsmodell.customOpsjonsmodellNavn,
        )
        execute(queryOf(query, params))

        upsertAdministratorer(avtale.id, avtale.detaljerDbo.administratorer)
        upsertArrangor(avtale.id, avtale.detaljerDbo.arrangor)
        OpplaringKategoriseringQueries(session).upsert(avtale.id, avtale.detaljerDbo.opplaringKategorisering)
        updateVeilederinfo(avtale.id, avtale.veilederinformasjonDbo)
        updatePersonvern(avtale.id, avtale.personvernDbo)
        avtale.prismodeller.forEach { upsertPrismodell(avtale.id, it) }
    }

    fun updateDetaljer(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
    ) = withTransaction(session) {
        upsertDetaljer(avtaleId, detaljerDbo)
        upsertAdministratorer(avtaleId, detaljerDbo.administratorer)
        upsertArrangor(avtaleId, detaljerDbo.arrangor)
        OpplaringKategoriseringQueries(session).upsert(avtaleId, detaljerDbo.opplaringKategorisering)
    }

    fun updatePersonvern(avtaleId: UUID, personvernDbo: PersonvernDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val updatePersonvernBekreftet = """
            update avtale
            set personvern_bekreftet = :personvern_bekreftet::boolean
            where id = :id::uuid
        """.trimIndent()
        execute(
            queryOf(
                updatePersonvernBekreftet,
                mapOf("id" to avtaleId, "personvern_bekreftet" to personvernDbo.personvernBekreftet),
            ),
        )

        @Language("PostgreSQL")
        val updatePersonopplysninger = """
            insert into avtale_personopplysning (avtale_id, personopplysning)
            values (?::uuid, ?)
            on conflict do nothing
        """.trimIndent()
        batchPreparedStatement(
            updatePersonopplysninger,
            personvernDbo.personopplysninger.map { listOf(avtaleId, it.name) },
        )

        @Language("PostgreSQL")
        val updatePersonopplysningAnnet = """
            insert into avtale_personopplysning (avtale_id, personopplysning, beskrivelse)
            values (?::uuid, ?, ?)
            on conflict (avtale_id, personopplysning) do update set beskrivelse = excluded.beskrivelse
        """.trimIndent()
        if (personvernDbo.annetChecked) {
            execute(
                queryOf(
                    updatePersonopplysningAnnet,
                    avtaleId,
                    Personopplysning.Type.ANNET.name,
                    personvernDbo.annetBeskrivelse,
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
                createTextArray(
                    personvernDbo.personopplysninger
                        .plus(
                            if (personvernDbo.annetChecked) {
                                Personopplysning.Type.ANNET
                            } else {
                                null
                            },
                        )
                        .mapNotNull { it?.name },
                ),
            ),
        )
    }

    fun updateVeilederinfo(avtaleId: UUID, veilederinformasjonDbo: VeilederinformasjonDbo) = withTransaction(session) {
        veilederinformasjonDbo.redaksjoneltInnhold?.let { upsertRedaksjoneltInnhold(avtaleId, it) }
        upsertNavEnheter(avtaleId, veilederinformasjonDbo.navEnheter)
    }

    private fun upsertAdministratorer(avtaleId: UUID, administratorer: List<NavIdent>) = withTransaction(session) {
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

    private fun upsertArrangor(avtaleId: UUID, arrangor: AvtaleArrangorDbo?) = withTransaction(session) {
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
                arrangor?.underenheter?.let { createUuidArray(it) },
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
                arrangor?.kontaktpersoner?.let { createUuidArray(it) },
            ),
        )
    }

    private fun upsertDetaljer(id: UUID, detaljer: DetaljerDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set navn = :navn,
                sakarkiv_nummer = :sakarkiv_nummer,
                avtaletype = :avtaletype::avtaletype,
                start_dato = :start_dato,
                slutt_dato = :slutt_dato,
                opsjon_maks_varighet = :opsjon_maks_varighet,
                opsjonsmodell = coalesce(:opsjonsmodell::opsjonsmodell, opsjonsmodell),
                opsjon_custom_opsjonsmodell_navn = :opsjon_custom_opsjonsmodell_navn,
                arrangor_hovedenhet_id = :arrangor_hovedenhet_id,
                status = :status::avtale_status
             where id = :id::uuid
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "navn" to detaljer.navn,
            "sakarkiv_nummer" to detaljer.sakarkivNummer?.value,
            "arrangor_hovedenhet_id" to detaljer.arrangor?.hovedenhet,
            "start_dato" to detaljer.startDato,
            "slutt_dato" to detaljer.sluttDato,
            "avtaletype" to detaljer.avtaletype.name,
            "status" to detaljer.status.name,
            "opsjonsmodell" to detaljer.opsjonsmodell.type.name,
            "opsjon_maks_varighet" to detaljer.opsjonsmodell.opsjonMaksVarighet,
            "opsjon_custom_opsjonsmodell_navn" to detaljer.opsjonsmodell.customOpsjonsmodellNavn,
        )
        execute(queryOf(query, params))
    }

    private fun upsertRedaksjoneltInnhold(id: UUID, redaksjoneltInnhold: RedaksjoneltInnholdDbo) {
        @Language("PostgreSQL")
        val query = """
                update avtale
                set
                    beskrivelse = :beskrivelse,
                    faneinnhold = :faneinnhold::jsonb
                 where id = :id::uuid
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "beskrivelse" to redaksjoneltInnhold.beskrivelse,
            "faneinnhold" to redaksjoneltInnhold.faneinnhold.let { Json.encodeToString<Faneinnhold?>(it) },
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

    fun upsertPrismodell(avtaleId: UUID, prismodellId: UUID) {
        @Language("PostgreSQL")
        val updateAvtalenummer = """
            insert into avtale_prismodell (avtale_id, prismodell_id)
            values (:avtale_id, :prismodell_id)
            on conflict (avtale_id, prismodell_id) do nothing
        """.trimIndent()
        val params = mapOf("avtale_id" to avtaleId, "prismodell_id" to prismodellId)
        session.execute(queryOf(updateAvtalenummer, params))
    }

    fun deletePrismodell(avtaleId: UUID, prismodellId: UUID) {
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

    fun getOrError(id: UUID): Avtale = checkNotNull(get(id)) { "Avtale med id=$id mangler" }

    fun get(id: UUID): Avtale? {
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
              and (:nav_enheter::text[] is null or (
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter) or
                   arena_nav_enhet_enhetsnummer in (select enhetsnummer
                                                    from nav_enhet
                                                    where overordnet_enhet = any (:nav_enheter))))
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
        avtalenummer = string("avtalenummer"),
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
            ?.let { Json.decodeFromString<List<Avtale.ArrangorUnderenhet>>(it) }
            .orEmpty()
        val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<Avtale.ArrangorKontaktperson>>(it) }
            .orEmpty()
        Avtale.ArrangorHovedenhet(
            id = id,
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
            navn = string("arrangor_hovedenhet_navn"),
            slettet = boolean("arrangor_hovedenhet_slettet"),
            underenheter = underenheter,
            kontaktpersoner = arrangorKontaktpersoner,
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

    return Avtale(
        id = uuid("id"),
        tiltakstype = Avtale.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        navn = string("navn"),
        avtalenummer = string("avtalenummer"),
        sakarkivNummer = stringOrNull("sakarkiv_nummer")?.let { SakarkivNummer(it) },
        arrangor = arrangor,
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        avtaletype = Avtaletype.valueOf(string("avtaletype")),
        status = status,
        administratorer = toAdministratorer(),
        navEnheter = toNavEnheter().map { it.enhetsnummer }.toSet(),
        beskrivelse = stringOrNull("beskrivelse"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        personopplysninger = toPersonopplysninger(),
        personvernBekreftet = boolean("personvern_bekreftet"),
        opplaring = toOpplaringKategoriseringDetaljer()?.toOpplaringKategorisering(),
        opsjonsmodell = toOpsjonsmodell(),
        opsjonerRegistrert = toOpsjonerRegistrert(),
        prismodeller = toPrismodeller(),
    )
}

private fun Row.toNavEnheter(): List<NavEnhetDto> = stringOrNull("nav_enheter_json")
    ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
    ?: emptyList()

private fun Row.toAdministratorer(): List<Avtale.Administrator> = stringOrNull("administratorer_json")
    ?.let { Json.decodeFromString<List<Avtale.Administrator>>(it) }
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
