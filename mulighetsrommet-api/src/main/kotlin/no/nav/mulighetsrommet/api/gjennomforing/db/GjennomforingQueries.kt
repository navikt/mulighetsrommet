package no.nav.mulighetsrommet.api.gjennomforing.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktperson
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.*
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.*
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
                deltidsprosent,
                estimert_ventetid_verdi,
                estimert_ventetid_enhet,
                tilgjengelig_for_arrangor_dato
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
                deltidsprosent                     = excluded.deltidsprosent,
                estimert_ventetid_verdi            = excluded.estimert_ventetid_verdi,
                estimert_ventetid_enhet            = excluded.estimert_ventetid_enhet,
                tilgjengelig_for_arrangor_dato = excluded.tilgjengelig_for_arrangor_dato
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
            gjennomforing.navEnheter.map { mapOf("id" to gjennomforing.id, "enhet_id" to it.value) },
        )

        execute(
            queryOf(
                deleteEnheter,
                gjennomforing.id,
                createArrayOfValue(gjennomforing.navEnheter) { it.value },
            ),
        )

        val kontaktpersoner = gjennomforing.kontaktpersoner.map { kontakt ->
            mapOf(
                "id" to gjennomforing.id,
                "enheter" to createArrayOfValue(kontakt.navEnheter) { it.value },
                "nav_ident" to kontakt.navIdent.value,
                "beskrivelse" to kontakt.beskrivelse,
            )
        }
        batchPreparedNamedStatement(upsertKontaktperson, kontaktpersoner)

        execute(
            queryOf(
                deleteKontaktpersoner,
                gjennomforing.id,
                createArrayOfValue(gjennomforing.kontaktpersoner) { it.navIdent.value },
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

    fun updateArenaData(id: UUID, tiltaksnummer: String, arenaAnsvarligEnhet: String?) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing set
                tiltaksnummer = :tiltaksnummer, arena_ansvarlig_enhet = :arena_ansvarlig_enhet
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "tiltaksnummer" to tiltaksnummer,
            "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        )

        session.execute(queryOf(query, params))
    }

    fun get(id: UUID): GjennomforingDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from gjennomforing_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforingDto() }
    }

    fun getPrismodell(id: UUID): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select avtale.prismodell
            from gjennomforing
            join avtale on gjennomforing.avtale_id = avtale.id
            where gjennomforing.id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { row ->
            row.stringOrNull("prismodell")?.let { Prismodell.valueOf(it) }
        }
    }

    fun getUpdatedAt(id: UUID): LocalDateTime {
        @Language("PostgreSQL")
        val query = """
            select updated_at from gjennomforing where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.localDateTime("updated_at") }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        search: String? = null,
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstypeIder: List<UUID> = emptyList(),
        statuser: List<GjennomforingStatus> = emptyList(),
        sortering: String? = null,
        sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<Organisasjonsnummer> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        publisert: Boolean? = null,
        koordinatorNavIdent: NavIdent? = null,
    ): PaginatedResult<GjennomforingDto> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "slutt_dato_cutoff" to sluttDatoGreaterThanOrEqualTo,
            "avtale_id" to avtaleId,
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_orgnrs" to arrangorOrgnr.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "statuser" to statuser.ifEmpty { null }?.let { createTextArray(statuser) },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "koordinator_nav_ident" to koordinatorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
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
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter)))
              and ((:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb) or (:koordinator_nav_ident::text is null or koordinator_json @> :koordinator_nav_ident::jsonb))
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and (tiltakstype_tiltakskode is not null)
              and (:statuser::text[] is null or status = any(:statuser))
              and (:publisert::boolean is null or publisert = :publisert::boolean)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toGjennomforingDto() }
            .runWithSession(this)
    }

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }

    fun setPublisert(id: UUID, publisert: Boolean): Int {
        @Language("PostgreSQL")
        val query = """
           update gjennomforing
           set publisert = ?
           where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, publisert, id))
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean): Int {
        @Language("PostgreSQL")
        val query = """
           update gjennomforing
           set apent_for_pamelding = ?
           where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, apentForPamelding, id))
    }

    fun settilgjengeligForArrangorDato(id: UUID, date: LocalDate): Int {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set tilgjengelig_for_arrangor_dato = ?
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, date, id))
    }

    fun setAvtaleId(gjennomforingId: UUID, avtaleId: UUID?): Int {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return session.update(queryOf(query, avtaleId, gjennomforingId))
    }

    fun setAvsluttet(id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak?): Int {
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

        return session.update(queryOf(query, params))
    }

    fun frikobleKontaktpersonFraGjennomforing(kontaktpersonId: UUID, gjennomforingId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete
            from gjennomforing_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and gjennomforing_id = ?::uuid
        """.trimIndent()

        session.update(queryOf(query, kontaktpersonId, gjennomforingId))
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing_stengt_hos_arrangor (gjennomforing_id, periode, beskrivelse)
            values (:gjennomforing_id::uuid, :periode::daterange, :beskrivelse)
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to id,
            "periode" to periode.toDaterange(),
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

    fun insertKoordinatorForGjennomforing(
        id: UUID,
        navIdent: NavIdent,
        gjennomforingId: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing_koordinator(id, nav_ident, gjennomforing_id)
            values(:id::uuid, :nav_ident, :gjennomforing_id::uuid)
            on conflict (nav_ident, gjennomforing_id) do nothing
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "nav_ident" to navIdent.value,
            "gjennomforing_id" to gjennomforingId,
        )
        session.execute(queryOf(query, params))
    }

    fun deleteKoordinatorForGjennomforing(
        id: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing_koordinator
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun getOppgaveData(
        tiltakskoder: Set<Tiltakskode>,
    ): List<GjennomforingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                gjennomforing.id,
                gjennomforing.navn,
                gjennomforing.updated_at,
                tiltakstype.tiltakskode                                        as tiltakstype_tiltakskode,
                tiltakstype.navn                                               as tiltakstype_navn,
                (
                    select jsonb_agg(
                        jsonb_build_object(
                            'enhetsnummer', nav_enhet.enhetsnummer,
                            'navn', nav_enhet.navn,
                            'type', nav_enhet.type,
                            'status', nav_enhet.status,
                            'overordnetEnhet', nav_enhet.overordnet_enhet
                        )
                    )
                    from gjennomforing_nav_enhet
                    join nav_enhet on nav_enhet.enhetsnummer = gjennomforing_nav_enhet.enhetsnummer
                    where gjennomforing_nav_enhet.gjennomforing_id = gjennomforing.id
                ) as nav_enheter_json
            from gjennomforing
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
                left join gjennomforing_administrator on gjennomforing_administrator.gjennomforing_id = gjennomforing.id
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and tiltaksgjennomforing_status(gjennomforing.start_dato, gjennomforing.slutt_dato, gjennomforing.avsluttet_tidspunkt) = 'GJENNOMFORES'
            group by gjennomforing.id, tiltakstype.tiltakskode, tiltakstype.navn
            having count(gjennomforing_administrator.nav_ident) = 0
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(query, params)) {
            val navEnheter = it.stringOrNull("nav_enheter_json")
                ?.let { Json.decodeFromString<List<NavEnhetDbo>>(it) }
                ?: emptyList()
            val kontorstruktur = fromNavEnheter(navEnheter)
            val region = kontorstruktur.firstOrNull()?.region

            GjennomforingOppgaveData(
                id = it.uuid("id"),
                navn = it.string("navn"),
                updatedAt = it.localDateTime("updated_at"),
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakstype_tiltakskode")),
                tiltakstypeNavn = it.string("tiltakstype_navn"),
                navEnhet = region?.let {
                    GjennomforingOppgaveData.NavEnhet(
                        enhetsnummer = region.enhetsnummer,
                        navn = region.navn,
                    )
                },
            )
        }
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
        "deltidsprosent" to deltidsprosent,
        "estimert_ventetid_verdi" to estimertVentetidVerdi,
        "estimert_ventetid_enhet" to estimertVentetidEnhet,
        "tilgjengelig_for_arrangor_fra_dato" to tilgjengeligForArrangorDato,
    )

    private fun Row.toGjennomforingDto(): GjennomforingDto {
        val administratorer = stringOrNull("administratorer_json")
            ?.let { Json.decodeFromString<List<GjennomforingDto.Administrator>>(it) }
            ?: emptyList()
        val navEnheter = stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetDbo>>(it) }
            ?: emptyList()
        val kontorstruktur = fromNavEnheter(navEnheter)

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
            lopenummer = string("lopenummer"),
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
            kontorstruktur = kontorstruktur,
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
            tilgjengeligForArrangorDato = localDateOrNull("tilgjengelig_for_arrangor_dato"),
            amoKategorisering = stringOrNull("amo_kategorisering_json")?.let { JsonIgnoreUnknownKeys.decodeFromString(it) },
            utdanningslop = utdanningslop,
            stengt = stengt,
        )
    }
}

data class GjennomforingOppgaveData(
    val id: UUID,
    val navn: String,
    val navEnhet: NavEnhet?,
    val tiltakskode: Tiltakskode,
    val tiltakstypeNavn: String,
    val updatedAt: LocalDateTime,
) {
    data class NavEnhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}
