package no.nav.mulighetsrommet.api.avtale.db

import PersonvernDbo
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleQueries(private val session: Session) {

    fun upsert(avtale: AvtaleDbo) = withTransaction(session) {
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
                opsjon_maks_varighet,
                avtaletype,
                beskrivelse,
                faneinnhold,
                personvern_bekreftet,
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
                :opsjonMaksVarighet,
                :avtaletype::avtaletype,
                :beskrivelse,
                :faneinnhold::jsonb,
                :personvern_bekreftet,
                :opsjonsmodell::opsjonsmodell,
                :opsjonCustomOpsjonsmodellNavn
            ) on conflict (id) do update set
                navn                        = excluded.navn,
                tiltakstype_id              = excluded.tiltakstype_id,
                sakarkiv_nummer             = excluded.sakarkiv_nummer,
                arrangor_hovedenhet_id      = excluded.arrangor_hovedenhet_id,
                start_dato                  = excluded.start_dato,
                slutt_dato                  = excluded.slutt_dato,
                status                      = excluded.status,
                opsjon_maks_varighet        = excluded.opsjon_maks_varighet,
                avtaletype                  = excluded.avtaletype,
                beskrivelse                 = excluded.beskrivelse,
                faneinnhold                 = excluded.faneinnhold,
                personvern_bekreftet        = excluded.personvern_bekreftet,
                opsjonsmodell               = excluded.opsjonsmodell,
                opsjon_custom_opsjonsmodell_navn = excluded.opsjon_custom_opsjonsmodell_navn
        """.trimIndent()

        execute(queryOf(query, avtale.toSqlParameters()))

        AmoKategoriseringQueries.upsert(
            AmoKategoriseringQueries.Relation.AVTALE,
            avtale.id,
            avtale.detaljerDbo.amoKategorisering,
        )
        upsertArrangor(avtale.id, avtale.detaljerDbo.arrangor)
        upsertNavEnheter(avtale.id, avtale.veilederinformasjonDbo.navEnheter)
        upsertAdministratorer(avtale.id, avtale.detaljerDbo.administratorer)
        upsertUtdanningslop(avtale.id, avtale.detaljerDbo.utdanningslop)
        upsertPrismodell(avtale.id, avtale.prismodellDbo)
        upsertPersonopplysninger(avtale.id, avtale.personvernDbo.personopplysninger)
    }

    fun updateDetaljer(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
    ) = withTransaction(session) {
        upsertDetaljer(avtaleId, detaljerDbo)
        upsertAdministratorer(avtaleId, detaljerDbo.administratorer)
        upsertArrangor(avtaleId, detaljerDbo.arrangor)
        upsertUtdanningslop(avtaleId, detaljerDbo.utdanningslop)
    }

    fun updatePersonvern(avtaleId: UUID, personvernDbo: PersonvernDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val updatePersonvernBekreftet = """
                update avtale
                set
                    personvern_bekreftet = :personvern_bekreftet::boolean
                 where id = :id::uuid
        """.trimIndent()

        session.execute(
            queryOf(
                updatePersonvernBekreftet,
                mapOf("id" to avtaleId, "personvern_bekreftet" to personvernDbo.personvernBekreftet),
            ),
        )
        upsertPersonopplysninger(avtaleId, personvernDbo.personopplysninger)
    }

    fun updateVeilederinfo(avtaleId: UUID, veilederinformasjonDbo: VeilederinformasjonDbo) = withTransaction(session) {
        veilederinformasjonDbo.redaksjoneltInnhold?.let { upsertRedaksjoneltInnhold(avtaleId, it) }
        upsertNavEnheter(avtaleId, veilederinformasjonDbo.navEnheter)
    }

    private fun DetaljerDbo.params(id: UUID) = mapOf(
        "id" to id,
        "opphav" to ArenaMigrering.Opphav.TILTAKSADMINISTRASJON.name,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "sakarkiv_nummer" to sakarkivNummer?.value,
        "arrangor_hovedenhet_id" to arrangor?.hovedenhet,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "avtaletype" to avtaletype.name,
        "status" to status.name,
    ) + opsjonsmodell.params()

    private fun Opsjonsmodell.params() = mapOf(
        "opsjonsmodell" to type.name,
        "opsjon_maks_varighet" to opsjonMaksVarighet,
        "opsjon_custom_opsjonsmodell_navn" to customOpsjonsmodellNavn,
    )

    private fun RedaksjoneltInnholdDbo.params(id: UUID) = mapOf(
        "id" to id,
        "beskrivelse" to beskrivelse,
        "faneinnhold" to faneinnhold.let { Json.encodeToString(it) },
    )

    fun upsertAdministratorer(avtaleId: UUID, administratorer: List<NavIdent>) {
        @Language("PostgreSQL")
        val upsertAdministrator = """
             insert into avtale_administrator(avtale_id, nav_ident)
             values (?::uuid, ?)
             on conflict (avtale_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from avtale_administrator
             where avtale_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()

        session.batchPreparedStatement(upsertAdministrator, administratorer.map { listOf(avtaleId, it.value) })
        session.execute(
            queryOf(
                deleteAdministratorer,
                avtaleId,
                session.createArrayOfValue(administratorer) { it.value },
            ),
        )
    }

    fun upsertArrangor(avtaleId: UUID, arrangor: ArrangorDbo?) {
        @Language("PostgreSQL")
        val setArrangorUnderenhet = """
             insert into avtale_arrangor_underenhet (avtale_id, arrangor_id)
             values (?::uuid, ?::uuid)
             on conflict (avtale_id, arrangor_id) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from avtale_arrangor_underenhet
             where avtale_id = ?::uuid and not (arrangor_id = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into avtale_arrangor_kontaktperson (avtale_id, arrangor_kontaktperson_id)
            values (?::uuid, ?::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from avtale_arrangor_kontaktperson
            where avtale_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        arrangor?.underenheter?.let { underenheter ->
            session.batchPreparedStatement(setArrangorUnderenhet, underenheter.map { listOf(avtaleId, it) })
        }
        session.execute(
            queryOf(
                deleteUnderenheter,
                avtaleId,
                arrangor?.underenheter?.let { session.createUuidArray(it) },
            ),
        )
        arrangor?.kontaktpersoner?.let { kontaktpersoner ->
            session.batchPreparedStatement(upsertArrangorKontaktperson, kontaktpersoner.map { listOf(avtaleId, it) })
        }
        session.execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                avtaleId,
                arrangor?.kontaktpersoner?.let { session.createUuidArray(it) },
            ),
        )
    }

    fun upsertUtdanningslop(avtaleId: UUID, utdanningslop: UtdanningslopDbo?) {
        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from avtale_utdanningsprogram
            where avtale_id = ?::uuid
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into avtale_utdanningsprogram(
                avtale_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:avtale_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
        """.trimIndent()
        session.execute(queryOf(deleteUtdanningslop, avtaleId))

        utdanningslop?.let { utdanningslop ->
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "avtale_id" to avtaleId,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            session.batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }
    }

    fun upsertDetaljer(id: UUID, detaljer: DetaljerDbo) {
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

        session.execute(queryOf(query, detaljer.params(id)))
    }

    fun upsertRedaksjoneltInnhold(id: UUID, redaksjoneltInnhold: RedaksjoneltInnholdDbo) {
        @Language("PostgreSQL")
        val query = """
                update avtale
                set
                    beskrivelse = :beskrivelse,
                    faneinnhold = :faneinnhold::jsonb
                 where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, redaksjoneltInnhold.params(id)))
    }

    fun upsertNavEnheter(avtaleId: UUID, enheter: Set<NavEnhetNummer>) {
        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into avtale_nav_enhet (avtale_id, enhetsnummer)
             values (?::uuid, ?)
             on conflict (avtale_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from avtale_nav_enhet
             where avtale_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        session.batchPreparedStatement(upsertEnhet, enheter.map { listOf(avtaleId, it.value) })
        session.execute(queryOf(deleteEnheter, avtaleId, session.createArrayOfValue(enheter) { it.value }))
    }

    fun upsertPersonopplysninger(id: UUID, personopplysninger: List<Personopplysning>) = withTransaction(session) {
        @Language("PostgreSQL")
        val updatePersonopplysninger = """
                insert into avtale_personopplysning (avtale_id, personopplysning)
                values (?::uuid, ?::personopplysning)
                on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deletePersonopplysninger = """
                delete from avtale_personopplysning
                where avtale_id = ?::uuid and not (personopplysning = any (?))
        """.trimIndent()

        session.batchPreparedStatement(
            updatePersonopplysninger,
            personopplysninger.map { listOf<Any>(id, it.name) },
        )

        session.execute(
            queryOf(
                deletePersonopplysninger,
                id,
                session.createArrayOfPersonopplysning(personopplysninger),
            ),
        )
    }

    fun upsertPrismodell(avtaleId: UUID, dbo: PrismodellDbo) = withTransaction(session) {
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

        val params = mapOf(
            "avtale_id" to avtaleId,
            "id" to dbo.id,
            "prismodell" to dbo.type.name,
            "prisbetingelser" to dbo.prisbetingelser,
            "satser" to Json.encodeToString(dbo.satser),
        )
        execute(queryOf(query, params))
    }

    fun upsertAvtalenummer(id: UUID, avtalenummer: String) = withTransaction(session) {
        @Language("PostgreSQL")
        val updateAvtalenummer = """
                update avtale
                set avtalenummer = :avtalenummer
                where id = :id::uuid
        """.trimIndent()

        execute(
            queryOf(
                updateAvtalenummer,
                mapOf("id" to id, "avtalenummer" to avtalenummer),
            ),
        )
    }

    fun upsertArenaAvtale(avtale: ArenaAvtaleDbo) = withTransaction(session) {
        val arrangorId = single(
            queryOf("select id from arrangor where organisasjonsnummer = ?", avtale.arrangorOrganisasjonsnummer),
        ) { it.uuid("id") }.let { requireNotNull(it) }

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               arrangor_hovedenhet_id,
                               start_dato,
                               slutt_dato,
                               status,
                               opsjonsmodell,
                               arena_ansvarlig_enhet,
                               avtaletype,
                               opphav)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :arrangor_hovedenhet_id,
                    :start_dato,
                    :slutt_dato,
                    :status::avtale_status,
                    :opsjonsmodell::opsjonsmodell,
                    :arena_ansvarlig_enhet,
                    :avtaletype::avtaletype,
                    :opphav::opphav)
            on conflict (id) do update set navn                     = excluded.navn,
                                           tiltakstype_id           = excluded.tiltakstype_id,
                                           avtalenummer             = excluded.avtalenummer,
                                           arrangor_hovedenhet_id   = excluded.arrangor_hovedenhet_id,
                                           start_dato               = excluded.start_dato,
                                           slutt_dato               = excluded.slutt_dato,
                                           status                   = excluded.status,
                                           opsjonsmodell            = coalesce(avtale.opsjonsmodell, excluded.opsjonsmodell),
                                           arena_ansvarlig_enhet    = excluded.arena_ansvarlig_enhet,
                                           avtaletype               = excluded.avtaletype,
                                           opphav                   = coalesce(avtale.opphav, excluded.opphav)
        """.trimIndent()

        execute(queryOf(query, avtale.toSqlParameters(arrangorId)))
        upsertPrismodell(
            avtaleId = avtale.id,
            PrismodellDbo(
                id = UUID.randomUUID(),
                type = when (avtale.avtaletype) {
                    Avtaletype.FORHANDSGODKJENT -> PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
                    else -> PrismodellType.ANNEN_AVTALT_PRIS
                },
                prisbetingelser = avtale.prisbetingelser,
                satser = emptyList(),
            ),
        )
    }

    fun getOrError(id: UUID): Avtale = checkNotNull(get(id)) { "Avtale med id=$id mangler" }

    fun get(id: UUID): Avtale? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_avtale
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toAvtale() }
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
    ): Int = with(session) {
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
        return update(queryOf(query, params))
    }

    fun delete(id: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale
            where id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, id))
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
    ) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and avtale_id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, kontaktpersonId, avtaleId))
    }

    fun getAvtaleIdsByAdministrator(navIdent: NavIdent): List<UUID> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select avtale_id
            from avtale_administrator
            where nav_ident = ?
        """.trimIndent()

        return list(queryOf(query, navIdent.value)) { it.uuid("avtale_id") }
    }

    fun setSluttDato(avtaleId: UUID, sluttDato: LocalDate) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set slutt_dato = ?
            where id = ?::uuid
        """

        update(queryOf(query, sluttDato, avtaleId))
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.TILTAKSADMINISTRASJON.name,
        "id" to id,
        "navn" to detaljerDbo.navn,
        "tiltakstype_id" to detaljerDbo.tiltakstypeId,
        "sakarkiv_nummer" to detaljerDbo.sakarkivNummer?.value,
        "arrangor_hovedenhet_id" to detaljerDbo.arrangor?.hovedenhet,
        "start_dato" to detaljerDbo.startDato,
        "slutt_dato" to detaljerDbo.sluttDato,
        "status" to detaljerDbo.status.name,
        "avtaletype" to detaljerDbo.avtaletype.name,
        "beskrivelse" to veilederinformasjonDbo.redaksjoneltInnhold?.beskrivelse,
        "faneinnhold" to veilederinformasjonDbo.redaksjoneltInnhold?.faneinnhold?.let { Json.encodeToString(it) },
        "personvern_bekreftet" to personvernDbo.personvernBekreftet,
        "opsjonsmodell" to detaljerDbo.opsjonsmodell.type.name,
        "opsjonMaksVarighet" to detaljerDbo.opsjonsmodell.opsjonMaksVarighet,
        "opsjonCustomOpsjonsmodellNavn" to detaljerDbo.opsjonsmodell.customOpsjonsmodellNavn,
    )

    private fun ArenaAvtaleDbo.toSqlParameters(arrangorId: UUID): Map<String, Any?> {
        val avbruttTidspunkt = when (avslutningsstatus) {
            Avslutningsstatus.AVLYST -> startDato.atStartOfDay().minusDays(1)
            Avslutningsstatus.AVBRUTT -> startDato.atStartOfDay()
            Avslutningsstatus.AVSLUTTET -> null
            Avslutningsstatus.IKKE_AVSLUTTET -> null
        }
        return mapOf(
            "opphav" to ArenaMigrering.Opphav.ARENA.name,
            "id" to id,
            "navn" to navn,
            "tiltakstype_id" to tiltakstypeId,
            "avtalenummer" to avtalenummer,
            "arrangor_hovedenhet_id" to arrangorId,
            "start_dato" to startDato,
            "slutt_dato" to sluttDato,
            "status" to when (avslutningsstatus) {
                Avslutningsstatus.IKKE_AVSLUTTET -> AvtaleStatusType.AKTIV
                Avslutningsstatus.AVSLUTTET -> AvtaleStatusType.AVSLUTTET
                Avslutningsstatus.AVLYST, Avslutningsstatus.AVBRUTT -> AvtaleStatusType.AVBRUTT
            }.name,
            "opsjonsmodell" to when (avtaletype) {
                Avtaletype.FORHANDSGODKJENT, Avtaletype.OFFENTLIG_OFFENTLIG -> OpsjonsmodellType.VALGFRI_SLUTTDATO
                else -> OpsjonsmodellType.INGEN_OPSJONSMULIGHET
            }.name,
            "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
            "avtaletype" to avtaletype.name,
            "avbrutt_tidspunkt" to avbruttTidspunkt,
            "avbrutt_aarsaker" to if (avbruttTidspunkt != null) session.createTextArray(listOf("AVBRUTT_I_ARENA")) else null,
            "avbrutt_forklaring" to null,
            "prismodell" to when (avtaletype) {
                Avtaletype.FORHANDSGODKJENT -> PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
                else -> PrismodellType.ANNEN_AVTALT_PRIS
            }.name,
            "prisbetingelser" to prisbetingelser,
        )
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

        val prismodeller = string("prismodeller_json")
            .let { Json.decodeFromString<List<PrismodellDbo>>(it) }
        val prismodell = prismodeller.map { p ->
            when (p.type) {
                PrismodellType.ANNEN_AVTALT_PRIS ->
                    Prismodell.AnnenAvtaltPris(
                        id = p.id,
                        prisbetingelser = p.prisbetingelser,
                    )

                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK ->
                    Prismodell.ForhandsgodkjentPrisPerManedsverk(
                        id = p.id,
                    )

                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK ->
                    Prismodell.AvtaltPrisPerManedsverk(
                        id = p.id,
                        prisbetingelser = p.prisbetingelser,
                        satser = p.satser.toDto(),
                    )

                PrismodellType.AVTALT_PRIS_PER_UKESVERK ->
                    Prismodell.AvtaltPrisPerUkesverk(
                        id = p.id,
                        prisbetingelser = p.prisbetingelser,
                        satser = p.satser.toDto(),
                    )

                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK ->
                    Prismodell.AvtaltPrisPerHeleUkesverk(
                        id = p.id,
                        prisbetingelser = p.prisbetingelser,
                        satser = p.satser.toDto(),
                    )

                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER ->
                    Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker(
                        id = p.id,
                        prisbetingelser = p.prisbetingelser,
                        satser = p.satser.toDto(),
                    )
            }
        }.first()

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
            prismodell = prismodell,
        )
    }
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
