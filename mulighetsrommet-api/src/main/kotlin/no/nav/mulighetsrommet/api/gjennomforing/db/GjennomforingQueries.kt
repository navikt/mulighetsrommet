package no.nav.mulighetsrommet.api.gjennomforing.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktperson
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.withTransaction
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingQueries(private val session: Session) {
    fun upsert(gjennomforing: GjennomforingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing (
                id,
                navn,
                tiltakstype_id,
                arrangor_id,
                start_dato,
                slutt_dato,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                sted_for_gjennomforing,
                faneinnhold,
                beskrivelse,
                nav_region,
                deltidsprosent,
                estimert_ventetid_verdi,
                estimert_ventetid_enhet,
                tilgjengelig_for_arrangor_fra_og_med_dato
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :arrangor_id,
                :start_dato,
                :slutt_dato,
                :antall_plasser,
                :avtale_id,
                :oppstart::gjennomforing_oppstartstype,
                :opphav::opphav,
                :sted_for_gjennomforing,
                :faneinnhold::jsonb,
                :beskrivelse,
                :nav_region,
                :deltidsprosent,
                :estimert_ventetid_verdi,
                :estimert_ventetid_enhet,
                :tilgjengelig_for_arrangor_fra_dato
            )
            on conflict (id) do update set
                navn                               = excluded.navn,
                tiltakstype_id                     = excluded.tiltakstype_id,
                arrangor_id                        = excluded.arrangor_id,
                start_dato                         = excluded.start_dato,
                slutt_dato                         = excluded.slutt_dato,
                antall_plasser                     = excluded.antall_plasser,
                avtale_id                          = excluded.avtale_id,
                oppstart                           = excluded.oppstart,
                opphav                             = coalesce(gjennomforing.opphav, excluded.opphav),
                sted_for_gjennomforing             = excluded.sted_for_gjennomforing,
                faneinnhold                        = excluded.faneinnhold,
                beskrivelse                        = excluded.beskrivelse,
                nav_region                         = excluded.nav_region,
                deltidsprosent                     = excluded.deltidsprosent,
                estimert_ventetid_verdi            = excluded.estimert_ventetid_verdi,
                estimert_ventetid_enhet            = excluded.estimert_ventetid_enhet,
                tilgjengelig_for_arrangor_fra_og_med_dato = excluded.tilgjengelig_for_arrangor_fra_og_med_dato
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into gjennomforing_nav_enhet (gjennomforing_id, enhetsnummer)
             values (:id::uuid, :enhet_id)
             on conflict (gjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from gjennomforing_nav_enhet
             where gjennomforing_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertAdministrator = """
             insert into gjennomforing_administrator (gjennomforing_id, nav_ident)
             values (:id::uuid, :nav_ident)
             on conflict (gjennomforing_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from gjennomforing_administrator
             where gjennomforing_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertKontaktperson = """
            insert into gjennomforing_kontaktperson (
                gjennomforing_id,
                enheter,
                kontaktperson_nav_ident,
                beskrivelse
            )
            values (:id::uuid, :enheter, :nav_ident, :beskrivelse)
            on conflict (gjennomforing_id, kontaktperson_nav_ident) do update set
                enheter = :enheter,
                beskrivelse = :beskrivelse
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from gjennomforing_kontaktperson
            where gjennomforing_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into gjennomforing_arrangor_kontaktperson (
                arrangor_kontaktperson_id,
                gjennomforing_id
            )
            values (:arrangor_kontaktperson_id::uuid, :gjennomforing_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from gjennomforing_arrangor_kontaktperson
            where gjennomforing_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from gjennomforing_utdanningsprogram
            where gjennomforing_id = ?::uuid
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into gjennomforing_utdanningsprogram(
                gjennomforing_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:gjennomforing_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
        """.trimIndent()

        execute(queryOf(query, gjennomforing.toSqlParameters()))

        batchPreparedNamedStatement(
            upsertAdministrator,
            gjennomforing.administratorer.map { administrator ->
                mapOf("id" to gjennomforing.id, "nav_ident" to administrator.value)
            },
        )

        execute(
            queryOf(
                deleteAdministratorer,
                gjennomforing.id,
                gjennomforing.administratorer.map { it.value }.let { createTextArray(it) },
            ),
        )

        batchPreparedNamedStatement(
            upsertEnhet,
            gjennomforing.navEnheter.map { enhetId ->
                mapOf("id" to gjennomforing.id, "enhet_id" to enhetId)
            },
        )

        execute(
            queryOf(
                deleteEnheter,
                gjennomforing.id,
                createTextArray(gjennomforing.navEnheter),
            ),
        )

        val kontaktpersoner = gjennomforing.kontaktpersoner.map { kontakt ->
            mapOf(
                "id" to gjennomforing.id,
                "enheter" to createTextArray(kontakt.navEnheter),
                "nav_ident" to kontakt.navIdent.value,
                "beskrivelse" to kontakt.beskrivelse,
            )
        }
        batchPreparedNamedStatement(upsertKontaktperson, kontaktpersoner)

        execute(
            queryOf(
                deleteKontaktpersoner,
                gjennomforing.id,
                gjennomforing.kontaktpersoner.map { it.navIdent.value }.let { createTextArray(it) },
            ),
        )

        val arrangorKontaktpersoner = gjennomforing.arrangorKontaktpersoner.map { person ->
            mapOf(
                "gjennomforing_id" to gjennomforing.id,
                "arrangor_kontaktperson_id" to person,
            )
        }
        batchPreparedNamedStatement(upsertArrangorKontaktperson, arrangorKontaktpersoner)

        execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                gjennomforing.id,
                createUuidArray(gjennomforing.arrangorKontaktpersoner),
            ),
        )

        AmoKategoriseringQueries(this).upsert(gjennomforing)

        execute(queryOf(deleteUtdanningslop, gjennomforing.id))

        gjennomforing.utdanningslop?.also { utdanningslop ->
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "gjennomforing_id" to gjennomforing.id,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }
    }

    fun updateArenaData(id: UUID, tiltaksnummer: String, arenaAnsvarligEnhet: String?) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing set
                tiltaksnummer = :tiltaksnummer, arena_ansvarlig_enhet = :arena_ansvarlig_enhet
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "tiltaksnummer" to tiltaksnummer, "arena_ansvarlig_enhet" to arenaAnsvarligEnhet)

        execute(queryOf(query, params))
    }

    fun get(id: UUID): GjennomforingDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from gjennomforing_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toTiltaksgjennomforingDto() }
    }

    fun getUpdatedAt(id: UUID): LocalDateTime? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select updated_at from gjennomforing where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.localDateTimeOrNull("updated_at") }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        search: String? = null,
        navEnheter: List<String> = emptyList(),
        tiltakstypeIder: List<UUID> = emptyList(),
        statuser: List<GjennomforingStatus> = emptyList(),
        sortering: String? = null,
        sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<Organisasjonsnummer> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        opphav: ArenaMigrering.Opphav? = null,
        publisert: Boolean? = null,
    ): PaginatedResult<GjennomforingDto> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "slutt_dato_cutoff" to sluttDatoGreaterThanOrEqualTo,
            "avtale_id" to avtaleId,
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createTextArray(it) },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_orgnrs" to arrangorOrgnr.ifEmpty { null }?.map { it.value }?.let { createTextArray(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createTextArray(statuser) },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "opphav" to opphav?.name,
            "publisert" to publisert,
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "tiltaksnummer-ascending" -> "tiltaksnummer asc"
            "tiltaksnummer-descending" -> "tiltaksnummer desc"
            "arrangor-ascending" -> "arrangor_navn asc"
            "arrangor-descending" -> "arrangor_navn desc"
            "tiltakstype-ascending" -> "tiltakstype_navn asc"
            "tiltakstype-descending" -> "tiltakstype_navn desc"
            "startdato-ascending" -> "start_dato asc"
            "startdato-descending" -> "start_dato desc"
            "sluttdato-ascending" -> "slutt_dato asc"
            "sluttdato-descending" -> "slutt_dato desc"
            "publisert-ascending" -> "publisert asc"
            "publisert-descending" -> "publisert desc"
            else -> "navn, id"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over () as total_count
            from gjennomforing_admin_dto_view
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
              and (:avtale_id::uuid is null or avtale_id = :avtale_id)
              and (:arrangor_ids::uuid[] is null or arrangor_id = any(:arrangor_ids))
              and (:arrangor_orgnrs::text[] is null or arrangor_organisasjonsnummer = any(:arrangor_orgnrs))
              and (:search::text is null or (fts @@ to_tsquery('norwegian', :search) or arrangor_navn ilike :search_arrangor))
              and (:nav_enheter::text[] is null or (
                   nav_region_enhetsnummer = any (:nav_enheter) or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter)))
              and (:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb)
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and (tiltakstype_tiltakskode is not null)
              and (:opphav::opphav is null or opphav = :opphav::opphav)
              and (:statuser::text[] is null or status = any(:statuser))
              and (:publisert::boolean is null or publisert = :publisert::boolean)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toTiltaksgjennomforingDto() }
            .runWithSession(this)
    }

    fun getGjennomforesInPeriodeUtenRefusjonskrav(periode: Periode): List<GjennomforingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select * from gjennomforing_admin_dto_view
            where
                (start_dato <= :periode_slutt) and
                (slutt_dato >= :periode_start or slutt_dato is null) and
                (avsluttet_tidspunkt > :periode_start or avsluttet_tidspunkt is null) and
                not exists (
                    select 1
                    from refusjonskrav
                        join refusjonskrav_beregning_aft ON refusjonskrav.id = refusjonskrav_beregning_aft.refusjonskrav_id
                    where refusjonskrav.gjennomforing_id = gjennomforing_admin_dto_view.id
                    and refusjonskrav_beregning_aft.periode && daterange(:periode_start, :periode_slutt)
                );
        """.trimIndent()

        val params = mapOf("periode_start" to periode.start, "periode_slutt" to periode.slutt)

        return list(queryOf(query, params)) { it.toTiltaksgjennomforingDto() }
    }

    fun delete(id: UUID): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, id))
    }

    fun setOpphav(id: UUID, opphav: ArenaMigrering.Opphav): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set opphav = ?::opphav
            where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, opphav.name, id))
    }

    fun setPublisert(id: UUID, publisert: Boolean): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
           update gjennomforing
           set publisert = ?
           where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, publisert, id))
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
           update gjennomforing
           set apent_for_pamelding = ?
           where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, apentForPamelding, id))
    }

    fun setTilgjengeligForArrangorFraOgMedDato(id: UUID, date: LocalDate): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set tilgjengelig_for_arrangor_fra_og_med_dato = ?
            where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, date, id))
    }

    fun setAvtaleId(gjennomforingId: UUID, avtaleId: UUID?): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return update(queryOf(query, avtaleId, gjennomforingId))
    }

    fun setAvsluttet(id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak?): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set avsluttet_tidspunkt = :tidspunkt,
                avbrutt_aarsak = :aarsak,
                publisert = false,
                apent_for_pamelding = false
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "tidspunkt" to tidspunkt, "aarsak" to aarsak?.name)

        return update(queryOf(query, params))
    }

    fun frikobleKontaktpersonFraGjennomforing(kontaktpersonId: UUID, gjennomforingId: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from gjennomforing_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and gjennomforing_id = ?::uuid
        """.trimIndent()

        update(queryOf(query, kontaktpersonId, gjennomforingId))
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing_stengt_hos_arrangor (gjennomforing_id, periode, beskrivelse)
            values (:gjennomforing_id::uuid, daterange(:periode_start, :periode_slutt), :beskrivelse)
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to id,
            "periode_start" to periode.start,
            "periode_slutt" to periode.slutt,
            "beskrivelse" to beskrivelse,
        )

        session.execute(queryOf(query, params))
    }

    fun deleteStengtHosArrangor(
        id: Int,
    ) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing_stengt_hos_arrangor
            where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    private fun GjennomforingDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.MR_ADMIN_FLATE.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "arrangor_id" to arrangorId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "sted_for_gjennomforing" to stedForGjennomforing,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
        "beskrivelse" to beskrivelse,
        "nav_region" to navRegion,
        "deltidsprosent" to deltidsprosent,
        "estimert_ventetid_verdi" to estimertVentetidVerdi,
        "estimert_ventetid_enhet" to estimertVentetidEnhet,
        "tilgjengelig_for_arrangor_fra_dato" to tilgjengeligForArrangorFraOgMedDato,
    )

    private fun Row.toTiltaksgjennomforingDto(): GjennomforingDto {
        val administratorer = stringOrNull("administratorer_json")
            ?.let { Json.decodeFromString<List<GjennomforingDto.Administrator>>(it) }
            ?: emptyList()
        val navEnheterDto = stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetDbo>>(it) }
            ?: emptyList()
        val kontaktpersoner = stringOrNull("nav_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<GjennomforingKontaktperson>>(it) }
            ?: emptyList()
        val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<ArrangorKontaktperson>>(it) }
            ?: emptyList()
        val stengt = stringOrNull("stengt_perioder_json")
            ?.let { Json.decodeFromString<List<GjennomforingDto.StengtPeriode>>(it) }
            ?: emptyList()
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")

        val utdanningslop = stringOrNull("utdanningslop_json")?.let {
            Json.decodeFromString<UtdanningslopDto>(it)
        }

        val status = GjennomforingStatus.valueOf(string("status"))
        val avbrutt = when (status) {
            GjennomforingStatus.AVBRUTT, GjennomforingStatus.AVLYST -> {
                val aarsak = AvbruttAarsak.fromString(string("avbrutt_aarsak"))
                AvbruttDto(
                    tidspunkt = localDateTime("avsluttet_tidspunkt"),
                    aarsak = aarsak,
                    beskrivelse = aarsak.beskrivelse,
                )
            }

            else -> null
        }

        return GjennomforingDto(
            id = uuid("id"),
            navn = string("navn"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            lopenummer = stringOrNull("lopenummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            status = GjennomforingStatusDto(status, avbrutt),
            apentForPamelding = boolean("apent_for_pamelding"),
            antallPlasser = int("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            createdAt = localDateTime("created_at"),
            deltidsprosent = double("deltidsprosent"),
            estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
                GjennomforingDto.EstimertVentetid(
                    verdi = int("estimert_ventetid_verdi"),
                    enhet = string("estimert_ventetid_enhet"),
                )
            },
            stedForGjennomforing = stringOrNull("sted_for_gjennomforing"),
            publisert = boolean("publisert"),
            navRegion = stringOrNull("nav_region_enhetsnummer")?.let {
                NavEnhetDbo(
                    enhetsnummer = it,
                    navn = string("nav_region_navn"),
                    type = Norg2Type.valueOf(string("nav_region_type")),
                    overordnetEnhet = stringOrNull("nav_region_overordnet_enhet"),
                    status = NavEnhetStatus.valueOf(string("nav_region_status")),
                )
            },
            navEnheter = navEnheterDto,
            arenaAnsvarligEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
                ArenaNavEnhet(
                    navn = stringOrNull("arena_nav_enhet_navn"),
                    enhetsnummer = it,
                )
            },
            kontaktpersoner = kontaktpersoner,
            administratorer = administratorer,
            arrangor = GjennomforingDto.ArrangorUnderenhet(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
                kontaktpersoner = arrangorKontaktpersoner,
            ),
            tiltakstype = GjennomforingDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
            ),
            tilgjengeligForArrangorFraOgMedDato = localDateOrNull("tilgjengelig_for_arrangor_fra_og_med_dato"),
            amoKategorisering = stringOrNull("amo_kategorisering_json")?.let { JsonIgnoreUnknownKeys.decodeFromString(it) },
            utdanningslop = utdanningslop,
            stengt = stengt,
        )
    }
}
