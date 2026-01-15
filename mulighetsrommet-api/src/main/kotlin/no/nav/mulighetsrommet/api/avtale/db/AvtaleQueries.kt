package no.nav.mulighetsrommet.api.avtale.db

import PersonvernDbo
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.avtale.model.toDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleQueries(private val session: Session) {
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
                :tiltakstype_id::uuid,
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
            "tiltakstype_id" to avtale.detaljerDbo.tiltakstypeId,
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
        upsertAmo(avtale.id, avtale.detaljerDbo.amoKategorisering)
        upsertUtdanningslop(avtale.id, avtale.detaljerDbo.utdanningslop)
        updateVeilederinfo(avtale.id, avtale.veilederinformasjonDbo)
        updatePersonvern(avtale.id, avtale.personvernDbo)
        upsertPrismodell(avtale.id, avtale.prismodeller)
    }

    fun updateDetaljer(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
    ) = withTransaction(session) {
        upsertDetaljer(avtaleId, detaljerDbo)
        upsertAdministratorer(avtaleId, detaljerDbo.administratorer)
        upsertArrangor(avtaleId, detaljerDbo.arrangor)
        upsertAmo(avtaleId, detaljerDbo.amoKategorisering)
        upsertUtdanningslop(avtaleId, detaljerDbo.utdanningslop)
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
            values (?::uuid, ?::personopplysning)
            on conflict do nothing
        """.trimIndent()
        batchPreparedStatement(
            updatePersonopplysninger,
            personvernDbo.personopplysninger.map { listOf<Any>(avtaleId, it.name) },
        )

        @Language("PostgreSQL")
        val deletePersonopplysninger = """
            delete from avtale_personopplysning
            where avtale_id = ?::uuid and not (personopplysning = any (?))
        """.trimIndent()
        execute(
            queryOf(
                deletePersonopplysninger,
                avtaleId,
                createArrayOfPersonopplysning(personvernDbo.personopplysninger),
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

    private fun upsertArrangor(avtaleId: UUID, arrangor: ArrangorDbo?) = withTransaction(session) {
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

    context(session: TransactionalSession)
    private fun upsertAmo(avtaleId: UUID, amo: AmoKategorisering?) {
        AmoKategoriseringQueries.upsert(AmoKategoriseringQueries.Relation.AVTALE, avtaleId, amo)
    }

    private fun upsertUtdanningslop(avtaleId: UUID, utdanningslop: UtdanningslopDbo?) = withTransaction(session) {
        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from avtale_utdanningsprogram
            where avtale_id = ?::uuid
        """.trimIndent()
        execute(queryOf(deleteUtdanningslop, avtaleId))

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into avtale_utdanningsprogram(
                avtale_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:avtale_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
        """.trimIndent()
        utdanningslop?.let { utdanningslop ->
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "avtale_id" to avtaleId,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }
    }

    private fun upsertDetaljer(id: UUID, detaljer: DetaljerDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set
                navn = :navn,
                tiltakstype_id = :tiltakstype_id,
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
            "opphav" to ArenaMigrering.Opphav.TILTAKSADMINISTRASJON.name,
            "navn" to detaljer.navn,
            "tiltakstype_id" to detaljer.tiltakstypeId,
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

    fun upsertPrismodell(avtaleId: UUID, prismodeller: List<PrismodellDbo>) {
        @Language("PostgreSQL")
        val deleteQuery = """
            delete from avtale_prismodell
            where avtale_id = ?::uuid
            and not (id = any (?::uuid[]))
        """.trimIndent()

        val prismodellIds = prismodeller.map { it.id }
        if (prismodellIds.isNotEmpty()) {
            session.execute(
                queryOf(
                    deleteQuery,
                    avtaleId,
                    session.createUuidArray(prismodellIds),
                ),
            )
        }
        @Language("PostgreSQL")
        val query = """
            insert into avtale_prismodell(id,
                                          avtale_id,
                                          prisbetingelser,
                                          prismodell_type,
                                          satser)
            values (:id::uuid,
                    :avtale_id::uuid,
                    :prisbetingelser,
                    :prismodell::prismodell,
                    :satser::jsonb)
            on conflict (id) do update set avtale_id       = excluded.avtale_id,
                                           prisbetingelser = excluded.prisbetingelser,
                                           prismodell_type = excluded.prismodell_type,
                                           satser          = excluded.satser
        """.trimIndent()

        val params = prismodeller.map { dbo ->
            mapOf(
                "avtale_id" to avtaleId,
                "id" to dbo.id,
                "prismodell" to dbo.type.name,
                "prisbetingelser" to dbo.prisbetingelser,
                "satser" to Json.encodeToString(dbo.satser),
            )
        }
        session.batchPreparedNamedStatement(query, params)
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

    fun getAll(
        pagination: Pagination = Pagination.all(),
        tiltakstypeIder: List<UUID> = emptyList(),
        search: String? = null,
        statuser: List<AvtaleStatusType> = emptyList(),
        avtaletyper: List<Avtaletype> = emptyList(),
        navEnheter: List<NavEnhetNummer> = emptyList(),
        sortering: String? = null,
        arrangorIds: List<UUID> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        personvernBekreftet: Boolean? = null,
    ): PaginatedResult<Avtale> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
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
            .mapPaginated { it.toAvtale() }
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
}

private fun Row.toAvtale(): Avtale {
    val startDato = localDate("start_dato")
    val sluttDato = localDateOrNull("slutt_dato")
    val personopplysninger = stringOrNull("personopplysninger_json")
        ?.let { Json.decodeFromString<List<Personopplysning>>(it) }
        ?: emptyList()
    val administratorer = stringOrNull("administratorer_json")
        ?.let { Json.decodeFromString<List<Avtale.Administrator>>(it) }
        ?: emptyList()
    val navEnheter = stringOrNull("nav_enheter_json")
        ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
        ?: emptyList()
    val kontorstruktur = fromNavEnheter(navEnheter)

    val opsjonerRegistrert = stringOrNull("opsjon_logg_json")
        ?.let { Json.decodeFromString<List<Avtale.OpsjonLoggDto>>(it) }
        ?: emptyList()

    val opsjonsmodell = Opsjonsmodell(
        type = OpsjonsmodellType.valueOf(string("opsjonsmodell")),
        opsjonMaksVarighet = localDateOrNull("opsjon_maks_varighet"),
        customOpsjonsmodellNavn = stringOrNull("opsjon_custom_opsjonsmodell_navn"),
    )
    val amoKategorisering = stringOrNull("amo_kategorisering_json")
        ?.let { JsonIgnoreUnknownKeys.decodeFromString<AmoKategorisering>(it) }

    val utdanningslop = stringOrNull("utdanningslop_json")
        ?.let { Json.decodeFromString<UtdanningslopDto>(it) }

    val arrangor = uuidOrNull("arrangor_hovedenhet_id")?.let { id ->
        val underenheter = stringOrNull("arrangor_underenheter_json")
            ?.let { Json.decodeFromString<List<Avtale.ArrangorUnderenhet>>(it) }
            ?: emptyList()
        val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<Avtale.ArrangorKontaktperson>>(it) } ?: emptyList()
        Avtale.ArrangorHovedenhet(
            id = id,
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
            navn = string("arrangor_hovedenhet_navn"),
            slettet = boolean("arrangor_hovedenhet_slettet"),
            underenheter = underenheter,
            kontaktpersoner = arrangorKontaktpersoner,
        )
    }

    val prismodeller = Json.decodeFromString<List<PrismodellDbo>>(string("prismodeller_json")).map { prismodell ->
        val satser = prismodell.satser?.toDto() ?: listOf()
        when (prismodell.type) {
            PrismodellType.ANNEN_AVTALT_PRIS -> Prismodell.AnnenAvtaltPris(
                id = prismodell.id,
                prisbetingelser = prismodell.prisbetingelser,
            )

            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> Prismodell.ForhandsgodkjentPrisPerManedsverk(
                id = prismodell.id,
            )

            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK -> Prismodell.AvtaltPrisPerManedsverk(
                id = prismodell.id,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
            )

            PrismodellType.AVTALT_PRIS_PER_UKESVERK -> Prismodell.AvtaltPrisPerUkesverk(
                id = prismodell.id,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
            )

            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK -> Prismodell.AvtaltPrisPerHeleUkesverk(
                id = prismodell.id,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
            )

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker(
                id = prismodell.id,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
            )
        }
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
        navn = string("navn"),
        avtalenummer = stringOrNull("avtalenummer"),
        sakarkivNummer = stringOrNull("sakarkiv_nummer")?.let { SakarkivNummer(it) },
        startDato = startDato,
        sluttDato = sluttDato,
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
        avtaletype = Avtaletype.valueOf(string("avtaletype")),
        status = status,
        beskrivelse = stringOrNull("beskrivelse"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        administratorer = administratorer,
        kontorstruktur = kontorstruktur,
        arrangor = arrangor,
        arenaAnsvarligEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
            ArenaNavEnhet(
                navn = stringOrNull("arena_nav_enhet_navn"),
                enhetsnummer = it,
            )
        },
        tiltakstype = Avtale.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        personopplysninger = personopplysninger,
        personvernBekreftet = boolean("personvern_bekreftet"),
        opsjonsmodell = opsjonsmodell,
        opsjonerRegistrert = opsjonerRegistrert.sortedBy { it.createdAt },
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
        prismodeller = prismodeller,
    )
}

fun Session.createArrayOfAvtaleStatus(
    avtaletypes: List<AvtaleStatusType>,
): Array = createArrayOf("avtale_status", avtaletypes)

fun Session.createArrayOfAvtaletype(
    avtaletypes: List<Avtaletype>,
): Array = createArrayOf("avtaletype", avtaletypes)

fun Session.createArrayOfPersonopplysning(
    personopplysnings: List<Personopplysning>,
): Array = createArrayOf("personopplysning", personopplysnings)
