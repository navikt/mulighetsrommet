package no.nav.mulighetsrommet.api.gjennomforing.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.model.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.withTransaction
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
                status,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                oppmote_sted,
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
                :status::gjennomforing_status,
                :antall_plasser,
                :avtale_id,
                :oppstart::gjennomforing_oppstartstype,
                :opphav::opphav,
                :oppmote_sted,
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
                status                             = excluded.status,
                antall_plasser                     = excluded.antall_plasser,
                avtale_id                          = excluded.avtale_id,
                oppstart                           = excluded.oppstart,
                opphav                             = coalesce(gjennomforing.opphav, excluded.opphav),
                oppmote_sted                       = excluded.oppmote_sted,
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
                kontaktperson_nav_ident,
                beskrivelse
            )
            values (:id::uuid, :nav_ident, :beskrivelse)
            on conflict (gjennomforing_id, kontaktperson_nav_ident) do update set
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

        AmoKategoriseringQueries.upsert(
            AmoKategoriseringQueries.Relation.GJENNOMFORING,
            gjennomforing.id,
            gjennomforing.amoKategorisering,
        )

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
            update gjennomforing
            set arena_tiltaksnummer = :arena_tiltaksnummer,
                arena_ansvarlig_enhet = :arena_ansvarlig_enhet
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "arena_tiltaksnummer" to tiltaksnummer,
            "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        )

        session.execute(queryOf(query, params))
    }

    fun getOrError(id: UUID): Gjennomforing {
        return checkNotNull(get(id)) { "Gjennomføring med id $id finnes ikke" }
    }

    fun get(id: UUID): Gjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforingDto() }
    }

    fun getPrismodell(id: UUID): PrismodellType? {
        @Language("PostgreSQL")
        val query = """
            select avtale.prismodell
            from gjennomforing
            join avtale on gjennomforing.avtale_id = avtale.id
            where gjennomforing.id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { row ->
            PrismodellType.valueOf(row.string("prismodell"))
        }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        search: String? = null,
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstypeIder: List<UUID> = emptyList(),
        statuser: List<GjennomforingStatusType> = emptyList(),
        sortering: String? = null,
        sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<Organisasjonsnummer> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        publisert: Boolean? = null,
        koordinatorNavIdent: NavIdent? = null,
        prismodeller: List<PrismodellType> = emptyList(),
    ): PaginatedResult<GjennomforingKompakt> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "slutt_dato_cutoff" to sluttDatoGreaterThanOrEqualTo,
            "avtale_id" to avtaleId,
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_orgnrs" to arrangorOrgnr.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOf("gjennomforing_status", statuser) },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "koordinator_nav_ident" to koordinatorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "publisert" to publisert,
            "prismodeller" to prismodeller.ifEmpty { null }?.let { createArrayOf("prismodell", prismodeller) },
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "lopenummer-ascending" -> "lopenummer asc"
            "lopenummer-descending" -> "lopenummer desc"
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
            select id,
                   lopenummer,
                   navn,
                   start_dato,
                   slutt_dato,
                   status,
                   avsluttet_tidspunkt,
                   avbrutt_aarsaker,
                   avbrutt_forklaring,
                   publisert,
                   prismodell,
                   nav_enheter_json,
                   tiltakstype_id,
                   tiltakstype_tiltakskode,
                   tiltakstype_navn,
                   arrangor_id,
                   arrangor_organisasjonsnummer,
                   arrangor_navn,
                   count(*) over () as total_count
            from view_gjennomforing
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
              and (:avtale_id::uuid is null or avtale_id = :avtale_id)
              and (:arrangor_ids::uuid[] is null or arrangor_id = any(:arrangor_ids))
              and (:arrangor_orgnrs::text[] is null or arrangor_organisasjonsnummer = any(:arrangor_orgnrs))
              and (:search::text is null or (fts @@ to_tsquery('norwegian', :search) or arrangor_navn ilike :search_arrangor))
              and (:nav_enheter::text[] is null or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)))
              and ((:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb) or (:koordinator_nav_ident::text is null or koordinator_json @> :koordinator_nav_ident::jsonb))
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and (:statuser::text[] is null or status = any(:statuser))
              and (:publisert::boolean is null or publisert = :publisert::boolean)
              and (:prismodeller::text[] is null or prismodell = any(:prismodeller))
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toGjennomforingKompakt() }
            .runWithSession(this)
    }

    fun getByAvtale(avtaleId: UUID): List<Gjennomforing> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing
            where avtale_id = ?
        """.trimIndent()

        return session.list(queryOf(query, avtaleId)) { it.toGjennomforingDto() }
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

    fun setTilgjengeligForArrangorDato(id: UUID, date: LocalDate): Int {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set tilgjengelig_for_arrangor_dato = ?
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, date, id))
    }

    fun setStatus(
        id: UUID,
        status: GjennomforingStatusType,
        tidspunkt: LocalDateTime?,
        aarsaker: List<AvbrytGjennomforingAarsak>?,
        forklaring: String?,
    ): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing set
                status = :status::gjennomforing_status,
                avsluttet_tidspunkt = :tidspunkt,
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

    private fun GjennomforingDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.TILTAKSADMINISTRASJON.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "arrangor_id" to arrangorId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "status" to status.name,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "oppmote_sted" to oppmoteSted,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
        "beskrivelse" to beskrivelse,
        "deltidsprosent" to deltidsprosent,
        "estimert_ventetid_verdi" to estimertVentetidVerdi,
        "estimert_ventetid_enhet" to estimertVentetidEnhet,
        "tilgjengelig_for_arrangor_fra_dato" to tilgjengeligForArrangorDato,
    )
}

private fun Row.toGjennomforingKompakt(): GjennomforingKompakt {
    val navEnheter = stringOrNull("nav_enheter_json")
        ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
        ?: emptyList()

    return GjennomforingKompakt(
        id = uuid("id"),
        navn = string("navn"),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = toGjennomforingStatus(),
        publisert = boolean("publisert"),
        prismodell = stringOrNull("prismodell")?.let { PrismodellType.valueOf(it) },
        kontorstruktur = Kontorstruktur.fromNavEnheter(navEnheter),
        arrangor = GjennomforingKompakt.ArrangorUnderenhet(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
        ),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
    )
}

private fun Row.toGjennomforingDto(): Gjennomforing {
    val administratorer = stringOrNull("administratorer_json")
        ?.let { Json.decodeFromString<List<Gjennomforing.Administrator>>(it) }
        ?: emptyList()
    val navEnheter = stringOrNull("nav_enheter_json")
        ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
        ?: emptyList()

    val kontaktpersoner = stringOrNull("nav_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<GjennomforingKontaktperson>>(it) }
        ?: emptyList()
    val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<Gjennomforing.ArrangorKontaktperson>>(it) }
        ?: emptyList()
    val stengt = stringOrNull("stengt_perioder_json")
        ?.let { Json.decodeFromString<List<Gjennomforing.StengtPeriode>>(it) }
        ?: emptyList()
    val startDato = localDate("start_dato")
    val sluttDato = localDateOrNull("slutt_dato")

    val utdanningslop = stringOrNull("utdanningslop_json")?.let {
        Json.decodeFromString<UtdanningslopDto>(it)
    }

    return Gjennomforing(
        id = uuid("id"),
        navn = string("navn"),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        startDato = startDato,
        sluttDato = sluttDato,
        status = toGjennomforingStatus(),
        apentForPamelding = boolean("apent_for_pamelding"),
        antallPlasser = int("antall_plasser"),
        avtaleId = uuidOrNull("avtale_id"),
        avtalePrismodell = stringOrNull("prismodell")?.let { PrismodellType.valueOf(it) },
        oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
        beskrivelse = stringOrNull("beskrivelse"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
        oppdatertTidspunkt = localDateTime("oppdatert_tidspunkt"),
        deltidsprosent = double("deltidsprosent"),
        estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
            Gjennomforing.EstimertVentetid(
                verdi = int("estimert_ventetid_verdi"),
                enhet = string("estimert_ventetid_enhet"),
            )
        },
        publisert = boolean("publisert"),
        kontorstruktur = Kontorstruktur.fromNavEnheter(navEnheter),
        kontaktpersoner = kontaktpersoner,
        administratorer = administratorer,
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
            kontaktpersoner = arrangorKontaktpersoner,
        ),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        tilgjengeligForArrangorDato = localDateOrNull("tilgjengelig_for_arrangor_dato"),
        amoKategorisering = stringOrNull("amo_kategorisering_json")?.let { JsonIgnoreUnknownKeys.decodeFromString(it) },
        utdanningslop = utdanningslop,
        stengt = stengt,
        oppmoteSted = stringOrNull("oppmote_sted"),
        arena = Gjennomforing.ArenaData(
            tiltaksnummer = stringOrNull("arena_tiltaksnummer")?.let { Tiltaksnummer(it) },
            ansvarligNavEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
                ArenaNavEnhet(
                    navn = stringOrNull("arena_nav_enhet_navn"),
                    enhetsnummer = it,
                )
            },
        ),
    )
}

private fun Row.toGjennomforingStatus(): GjennomforingStatus {
    return when (GjennomforingStatusType.valueOf(string("status"))) {
        GjennomforingStatusType.GJENNOMFORES -> GjennomforingStatus.Gjennomfores

        GjennomforingStatusType.AVSLUTTET -> GjennomforingStatus.Avsluttet

        GjennomforingStatusType.AVBRUTT -> GjennomforingStatus.Avbrutt(
            tidspunkt = localDateTime("avsluttet_tidspunkt"),
            array<String>("avbrutt_aarsaker").map<String, AvbrytGjennomforingAarsak> {
                AvbrytGjennomforingAarsak.valueOf(
                    it,
                )
            },
            stringOrNull("avbrutt_forklaring"),
        )

        GjennomforingStatusType.AVLYST -> GjennomforingStatus.Avlyst(
            tidspunkt = localDateTime("avsluttet_tidspunkt"),
            array<String>("avbrutt_aarsaker").map<String, AvbrytGjennomforingAarsak> {
                AvbrytGjennomforingAarsak.valueOf(
                    it,
                )
            },
            stringOrNull("avbrutt_forklaring"),
        )
    }
}
