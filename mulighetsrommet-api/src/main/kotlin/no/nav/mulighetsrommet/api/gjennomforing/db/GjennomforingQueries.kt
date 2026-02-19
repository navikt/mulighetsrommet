package no.nav.mulighetsrommet.api.gjennomforing.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.db.toPrismodell
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArenaKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompakt
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class GjennomforingQueries(private val session: Session) {
    fun upsert(gjennomforing: GjennomforingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing (id,
                                       tiltakstype_id,
                                       arrangor_id,
                                       gjennomforing_type,
                                       oppstart,
                                       pamelding_type,
                                       navn,
                                       start_dato,
                                       slutt_dato,
                                       status,
                                       deltidsprosent,
                                       antall_plasser,
                                       avtale_id,
                                       prismodell_id,
                                       oppmote_sted,
                                       faneinnhold,
                                       beskrivelse,
                                       estimert_ventetid_verdi,
                                       estimert_ventetid_enhet,
                                       tilgjengelig_for_arrangor_dato,
                                       arena_tiltaksnummer,
                                       arena_ansvarlig_enhet)
            values (:id::uuid,
                    :tiltakstype_id::uuid,
                    :arrangor_id::uuid,
                    :gjennomforing_type::gjennomforing_type,
                    :oppstart::gjennomforing_oppstartstype,
                    :pamelding_type::pamelding_type,
                    :navn,
                    :start_dato,
                    :slutt_dato,
                    :status::gjennomforing_status,
                    :deltidsprosent,
                    :antall_plasser,
                    :avtale_id::uuid,
                    :prismodell_id::uuid,
                    :oppmote_sted,
                    :faneinnhold::jsonb,
                    :beskrivelse,
                    :estimert_ventetid_verdi,
                    :estimert_ventetid_enhet,
                    :tilgjengelig_for_arrangor_dato,
                    :arena_tiltaksnummer,
                    :arena_ansvarlig_enhet)
            on conflict (id) do update set tiltakstype_id = excluded.tiltakstype_id,
                                           arrangor_id    = excluded.arrangor_id,
                                           gjennomforing_type = excluded.gjennomforing_type,
                                           oppstart = excluded.oppstart,
                                           pamelding_type = excluded.pamelding_type,
                                           navn           = excluded.navn,
                                           start_dato     = excluded.start_dato,
                                           slutt_dato     = excluded.slutt_dato,
                                           status         = excluded.status,
                                           deltidsprosent = excluded.deltidsprosent,
                                           antall_plasser = excluded.antall_plasser,
                                           avtale_id  = excluded.avtale_id,
                                           prismodell_id  = excluded.prismodell_id,
                                           oppmote_sted  = excluded.oppmote_sted,
                                           faneinnhold  = excluded.faneinnhold,
                                           beskrivelse  = excluded.beskrivelse,
                                           estimert_ventetid_verdi  = excluded.estimert_ventetid_verdi,
                                           estimert_ventetid_enhet  = excluded.estimert_ventetid_enhet,
                                           tilgjengelig_for_arrangor_dato  = excluded.tilgjengelig_for_arrangor_dato,
                                           arena_tiltaksnummer = excluded.arena_tiltaksnummer,
                                           arena_ansvarlig_enhet = excluded.arena_ansvarlig_enhet
        """.trimIndent()

        val params = mapOf(
            "id" to gjennomforing.id,
            "tiltakstype_id" to gjennomforing.tiltakstypeId,
            "arrangor_id" to gjennomforing.arrangorId,
            "gjennomforing_type" to gjennomforing.type.name,
            "oppstart" to gjennomforing.oppstart.name,
            "pamelding_type" to gjennomforing.pameldingType.name,
            "navn" to gjennomforing.navn,
            "start_dato" to gjennomforing.startDato,
            "slutt_dato" to gjennomforing.sluttDato,
            "status" to gjennomforing.status.name,
            "deltidsprosent" to gjennomforing.deltidsprosent,
            "antall_plasser" to gjennomforing.antallPlasser,
            "avtale_id" to gjennomforing.avtaleId,
            "prismodell_id" to gjennomforing.prismodellId,
            "oppmote_sted" to gjennomforing.oppmoteSted,
            "faneinnhold" to gjennomforing.faneinnhold?.let { Json.encodeToString<Faneinnhold>(it) },
            "beskrivelse" to gjennomforing.beskrivelse,
            "estimert_ventetid_verdi" to gjennomforing.estimertVentetidVerdi,
            "estimert_ventetid_enhet" to gjennomforing.estimertVentetidEnhet,
            "tilgjengelig_for_arrangor_fra_dato" to gjennomforing.tilgjengeligForArrangorDato,
            "arena_tiltaksnummer" to gjennomforing.arenaTiltaksnummer?.value,
            "arena_ansvarlig_enhet" to gjennomforing.arenaAnsvarligEnhet,
        )

        session.execute(queryOf(query, params))
    }

    fun setNavEnheter(id: UUID, navEnheter: Set<NavEnhetNummer>) = with(session) {
        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into gjennomforing_nav_enhet (gjennomforing_id, enhetsnummer)
             values (:id::uuid, :enhet_id)
             on conflict (gjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()
        batchPreparedNamedStatement(upsertEnhet, navEnheter.map { mapOf("id" to id, "enhet_id" to it.value) })

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from gjennomforing_nav_enhet
             where gjennomforing_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()
        execute(queryOf(deleteEnheter, id, createArrayOfValue(navEnheter) { it.value }))
    }

    fun setAdministratorer(id: UUID, administratorer: Set<NavIdent>) = with(session) {
        @Language("PostgreSQL")
        val upsertAdministrator = """
             insert into gjennomforing_administrator (gjennomforing_id, nav_ident)
             values (:id::uuid, :nav_ident)
             on conflict (gjennomforing_id, nav_ident) do nothing
        """.trimIndent()
        batchPreparedNamedStatement(
            upsertAdministrator,
            administratorer.map { mapOf("id" to id, "nav_ident" to it.value) },
        )

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from gjennomforing_administrator
             where gjennomforing_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()
        execute(queryOf(deleteAdministratorer, id, createArrayOfValue(administratorer) { it.value }))
    }

    fun getAdministratorer(id: UUID): List<GjennomforingAvtaleDetaljer.Administrator>? {
        @Language("PostgreSQL")
        val query = """
            select administratorer_json
            from view_gjennomforing_avtale_detaljer
            where id = ?::uuid
        """.trimIndent()
        return session.single(queryOf(query, id)) { it.toAdministratorer() }
    }

    fun setKontaktpersoner(id: UUID, kontaktpersoner: Set<GjennomforingKontaktpersonDbo>) = with(session) {
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
        val params = kontaktpersoner.map { kontakt ->
            mapOf(
                "id" to id,
                "nav_ident" to kontakt.navIdent.value,
                "beskrivelse" to kontakt.beskrivelse,
            )
        }
        batchPreparedNamedStatement(upsertKontaktperson, params)

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from gjennomforing_kontaktperson
            where gjennomforing_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()
        execute(queryOf(deleteKontaktpersoner, id, createArrayOfValue(kontaktpersoner) { it.navIdent.value }))
    }

    fun setArrangorKontaktpersoner(id: UUID, arrangorKontaktpersoner: Set<UUID>) = with(session) {
        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into gjennomforing_arrangor_kontaktperson (
                arrangor_kontaktperson_id,
                gjennomforing_id
            )
            values (:arrangor_kontaktperson_id::uuid, :gjennomforing_id::uuid)
            on conflict do nothing
        """.trimIndent()
        val params = arrangorKontaktpersoner.map { person ->
            mapOf(
                "gjennomforing_id" to id,
                "arrangor_kontaktperson_id" to person,
            )
        }
        batchPreparedNamedStatement(upsertArrangorKontaktperson, params)

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from gjennomforing_arrangor_kontaktperson
            where gjennomforing_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()
        execute(queryOf(deleteArrangorKontaktpersoner, id, createUuidArray(arrangorKontaktpersoner)))
    }

    fun setUtdanningslop(id: UUID, utdanningslop: UtdanningslopDbo?) = with(session) {
        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from gjennomforing_utdanningsprogram
            where gjennomforing_id = ?::uuid
        """.trimIndent()
        execute(queryOf(deleteUtdanningslop, id))

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into gjennomforing_utdanningsprogram(
                gjennomforing_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:gjennomforing_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
        """.trimIndent()
        if (utdanningslop != null) {
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "gjennomforing_id" to id,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }
    }

    fun getUtdanningslop(id: UUID): UtdanningslopDto? {
        @Language("PostgreSQL")
        val query = """
            select utdanningslop_json
            from view_gjennomforing_avtale_detaljer
            where id = ?::uuid
        """.trimIndent()
        return session.single(queryOf(query, id)) { it.toUtdanningslopDto() }
    }

    fun setAmoKategorisering(id: UUID, amo: AmoKategorisering?): Unit = with(session) {
        AmoKategoriseringQueries.upsert(AmoKategoriseringQueries.Relation.GJENNOMFORING, id, amo)
    }

    fun setArenaData(dbo: GjennomforingArenaDataDbo) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set arena_tiltaksnummer = :arena_tiltaksnummer,
                arena_ansvarlig_enhet = :arena_ansvarlig_enhet
            where id = :id::uuid
        """.trimIndent()
        val params = mapOf(
            "id" to dbo.id,
            "arena_tiltaksnummer" to dbo.tiltaksnummer?.value,
            "arena_ansvarlig_enhet" to dbo.arenaAnsvarligEnhet,
        )
        session.execute(queryOf(query, params))
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
        typer: List<GjennomforingType> = listOf(),
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
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOf("gjennomforing_status", it) },
            "administrator_nav_ident" to administratorNavIdent?.value,
            "koordinator_nav_ident" to koordinatorNavIdent?.value,
            "publisert" to publisert,
            "gjennomforing_typer" to typer.ifEmpty { null }?.let { createArrayOf("gjennomforing_type", it) },
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
                   gjennomforing_type,
                   lopenummer,
                   navn,
                   start_dato,
                   slutt_dato,
                   status,
                   avbrutt_aarsaker,
                   avbrutt_forklaring,
                   publisert,
                   tiltakstype_id,
                   tiltakstype_tiltakskode,
                   tiltakstype_navn,
                   arrangor_id,
                   arrangor_organisasjonsnummer,
                   arrangor_navn,
                   nav_enheter_json,
                   count(*) over () as total_count
            from view_gjennomforing_kompakt
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
              and (:avtale_id::uuid is null or avtale_id = :avtale_id)
              and (:arrangor_ids::uuid[] is null or arrangor_id = any(:arrangor_ids))
              and (:arrangor_orgnrs::text[] is null or arrangor_organisasjonsnummer = any(:arrangor_orgnrs))
              and (:search::text is null or fts @@ to_tsquery('norwegian', :search))
              and (:nav_enheter::text[] is null or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)))
              and ((:administrator_nav_ident::text is null and :koordinator_nav_ident::text is null)
                    or :administrator_nav_ident in (select nav_ident from gjennomforing_administrator where gjennomforing_id = id)
                    or :koordinator_nav_ident in (select nav_ident from gjennomforing_koordinator where gjennomforing_id = view_gjennomforing_kompakt.id))
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and (:statuser::text[] is null or status = any(:statuser))
              and (:publisert::boolean is null or publisert = :publisert::boolean)
              and (:gjennomforing_typer::gjennomforing_type[] is null or gjennomforing_type = any(:gjennomforing_typer))
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toGjennomforingKompakt() }
            .runWithSession(this)
    }

    fun getByAvtale(avtaleId: UUID): List<GjennomforingAvtale> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing
            where avtale_id = ?
        """.trimIndent()

        return session.list(queryOf(query, avtaleId)) { it.toGjennomforingAvtale() }
    }

    fun getGjennomforingOrError(id: UUID): Gjennomforing {
        return checkNotNull(getGjennomforing(id)) { "Gjennomføring med id $id finnes ikke" }
    }

    fun getGjennomforing(id: UUID): Gjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforing() }
    }

    fun getGjennomforingAvtaleOrError(id: UUID): GjennomforingAvtale {
        return getGjennomforing(id) as? GjennomforingAvtale ?: error(
            "Gjennomføring med id $id er ikke av type GjennomforingAvtale",
        )
    }

    fun getGjennomforingEnkeltplassOrError(id: UUID): GjennomforingEnkeltplass {
        return getGjennomforing(id) as? GjennomforingEnkeltplass ?: error(
            "Gjennomføring med id $id er ikke av type GjennomforingEnkeltplass",
        )
    }

    fun getGjennomforingArenaOrError(id: UUID): GjennomforingArena {
        return getGjennomforing(id) as? GjennomforingArena ?: error(
            "Gjennomføring med id $id er ikke av type GjennomforingArena",
        )
    }

    fun getGjennomforingAvtaleDetaljerOrError(id: UUID): GjennomforingAvtaleDetaljer {
        return checkNotNull(getGjennomforingAvtaleDetaljer(id)) { "Detaljer om gjennomføring med id $id finnes ikke" }
    }

    fun getGjennomforingAvtaleDetaljer(id: UUID): GjennomforingAvtaleDetaljer? {
        @Language("PostgreSQL")
        val query = """
            select status,
                   avbrutt_aarsaker,
                   avbrutt_forklaring,
                   publisert,
                   oppmote_sted,
                   beskrivelse,
                   faneinnhold,
                   estimert_ventetid_verdi,
                   estimert_ventetid_enhet,
                   tilgjengelig_for_arrangor_dato,
                   administratorer_json,
                   nav_enheter_json,
                   nav_kontaktpersoner_json,
                   arrangor_kontaktpersoner_json,
                   utdanningslop_json,
                   amo_kategorisering_json
            from view_gjennomforing_avtale_detaljer
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforingAvtaleDetaljer() }
    }

    fun getPrismodell(id: UUID): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select prismodell.id as prismodell_id,
                   prismodell.prismodell_type,
                   prismodell.valuta as prismodell_valuta,
                   prismodell.prisbetingelser as prismodell_prisbetingelser,
                   prismodell.satser as prismodell_satser
            from gjennomforing
                join prismodell on prismodell.id = gjennomforing.prismodell_id
            where gjennomforing.id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toPrismodell() }
    }

    fun setFreeTextSearch(id: UUID, content: List<String>) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set fts = to_tsvector('norwegian',
                                  concat_ws(' ',
                                            lopenummer,
                                            regexp_replace(lopenummer, '/', ' '),
                                            coalesce(arena_tiltaksnummer, ''),
                                            :content
                                  )
                      )
            where id = :id
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "content" to content.joinToString(" "),
        )
        session.execute(queryOf(query, params))
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
        sluttDato: LocalDate?,
        aarsaker: List<AvbrytGjennomforingAarsak>?,
        forklaring: String?,
    ): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set status = :status::gjennomforing_status,
                slutt_dato = coalesce(:slutt_dato, slutt_dato),
                avbrutt_aarsaker = :aarsaker,
                avbrutt_forklaring = :forklaring
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
            "slutt_dato" to sluttDato,
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

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }
}

private fun Row.toGjennomforing(): Gjennomforing {
    return when (GjennomforingType.valueOf(string("gjennomforing_type"))) {
        GjennomforingType.AVTALE -> toGjennomforingAvtale()
        GjennomforingType.ENKELTPLASS -> toGjennomforingEnkeltplass()
        GjennomforingType.ARENA -> toGjennomforingArena()
    }
}

private fun Row.toGjennomforingKompakt(): GjennomforingKompakt {
    val arrangor = GjennomforingKompakt.ArrangorUnderenhet(
        id = uuid("arrangor_id"),
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        navn = string("arrangor_navn"),
    )
    val tiltakstype = GjennomforingKompakt.Tiltakstype(
        id = uuid("tiltakstype_id"),
        navn = string("tiltakstype_navn"),
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
    )
    return when (GjennomforingType.valueOf(string("gjennomforing_type"))) {
        GjennomforingType.AVTALE -> {
            GjennomforingAvtaleKompakt(
                id = uuid("id"),
                navn = string("navn"),
                lopenummer = Tiltaksnummer(string("lopenummer")),
                startDato = localDate("start_dato"),
                sluttDato = localDateOrNull("slutt_dato"),
                status = GjennomforingStatusType.valueOf(string("status")),
                publisert = boolean("publisert"),
                kontorstruktur = Kontorstruktur.fromNavEnheter(toNavEnheter()),
                arrangor = arrangor,
                tiltakstype = tiltakstype,
            )
        }

        GjennomforingType.ENKELTPLASS -> {
            GjennomforingEnkeltplassKompakt(
                id = uuid("id"),
                lopenummer = Tiltaksnummer(string("lopenummer")),
                startDato = localDate("start_dato"),
                sluttDato = localDateOrNull("slutt_dato"),
                status = GjennomforingStatusType.valueOf(string("status")),
                arrangor = arrangor,
                tiltakstype = tiltakstype,
            )
        }

        GjennomforingType.ARENA -> {
            GjennomforingArenaKompakt(
                id = uuid("id"),
                navn = string("navn"),
                lopenummer = Tiltaksnummer(string("lopenummer")),
                startDato = localDate("start_dato"),
                sluttDato = localDateOrNull("slutt_dato"),
                status = GjennomforingStatusType.valueOf(string("status")),
                arrangor = arrangor,
                tiltakstype = tiltakstype,
            )
        }
    }
}

private fun Row.toGjennomforingAvtale(): GjennomforingAvtale {
    return GjennomforingAvtale(
        id = uuid("id"),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        navn = string("navn"),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
        ),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.valueOf(string("status")),
        antallPlasser = int("antall_plasser"),
        apentForPamelding = boolean("apent_for_pamelding"),
        avtaleId = uuid("avtale_id"),
        prismodell = toPrismodell(),
        oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
        pameldingType = string("pamelding_type").let { GjennomforingPameldingType.valueOf(it) },
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
        opprettetTidspunkt = instant("opprettet_tidspunkt"),
        oppdatertTidspunkt = instant("oppdatert_tidspunkt"),
        deltidsprosent = double("deltidsprosent"),
        arena = stringOrNull("arena_tiltaksnummer")?.let {
            Gjennomforing.ArenaData(
                tiltaksnummer = Tiltaksnummer(it),
                ansvarligNavEnhet = stringOrNull("arena_nav_enhet_enhetsnummer"),
            )
        },
        kontorstruktur = Kontorstruktur.fromNavEnheter(toNavEnheter()),
        stengt = toStengtePerioder(),
    )
}

private fun Row.toGjennomforingAvtaleDetaljer(): GjennomforingAvtaleDetaljer {
    val kontaktpersoner = stringOrNull("nav_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtaleDetaljer.GjennomforingKontaktperson>>(it) }
        ?: emptyList()
    val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtaleDetaljer.ArrangorKontaktperson>>(it) }
        ?: emptyList()
    val amoKategorisering = stringOrNull("amo_kategorisering_json")
        ?.let { JsonIgnoreUnknownKeys.decodeFromString<AmoKategorisering>(it) }
    return GjennomforingAvtaleDetaljer(
        administratorer = toAdministratorer(),
        kontorstruktur = Kontorstruktur.fromNavEnheter(toNavEnheter()),
        kontaktpersoner = kontaktpersoner,
        oppmoteSted = stringOrNull("oppmote_sted"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        beskrivelse = stringOrNull("beskrivelse"),
        publisert = boolean("publisert"),
        estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
            GjennomforingAvtaleDetaljer.EstimertVentetid(
                verdi = int("estimert_ventetid_verdi"),
                enhet = string("estimert_ventetid_enhet"),
            )
        },
        tilgjengeligForArrangorDato = localDateOrNull("tilgjengelig_for_arrangor_dato"),
        amoKategorisering = amoKategorisering,
        utdanningslop = toUtdanningslopDto(),
        arrangorKontaktpersoner = arrangorKontaktpersoner,
        avbrytelse = when (GjennomforingStatusType.valueOf(string("status"))) {
            GjennomforingStatusType.GJENNOMFORES,
            GjennomforingStatusType.AVSLUTTET,
            -> null

            GjennomforingStatusType.AVBRUTT,
            GjennomforingStatusType.AVLYST,
            -> GjennomforingAvtaleDetaljer.Avbrytelse(
                array<String>("avbrutt_aarsaker").map { AvbrytGjennomforingAarsak.valueOf(it) },
                stringOrNull("avbrutt_forklaring"),
            )
        },

    )
}

private fun Row.toStengtePerioder(): List<GjennomforingAvtale.StengtPeriode> {
    return stringOrNull("stengt_perioder_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtale.StengtPeriode>>(it) }
        ?: emptyList()
}

private fun Row.toNavEnheter(): List<NavEnhetDto> {
    return stringOrNull("nav_enheter_json")?.let { Json.decodeFromString<List<NavEnhetDto>>(it) } ?: emptyList()
}

private fun Row.toAdministratorer(): List<GjennomforingAvtaleDetaljer.Administrator> {
    return stringOrNull("administratorer_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtaleDetaljer.Administrator>>(it) }
        ?: emptyList()
}

private fun Row.toUtdanningslopDto(): UtdanningslopDto? {
    return stringOrNull("utdanningslop_json")?.let { Json.decodeFromString<UtdanningslopDto>(it) }
}

private fun Row.toGjennomforingEnkeltplass(): GjennomforingEnkeltplass {
    return GjennomforingEnkeltplass(
        id = uuid("id"),
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        opprettetTidspunkt = instant("opprettet_tidspunkt"),
        oppdatertTidspunkt = instant("oppdatert_tidspunkt"),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
        ),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        arena = stringOrNull("arena_tiltaksnummer")?.let {
            Gjennomforing.ArenaData(
                tiltaksnummer = Tiltaksnummer(it),
                ansvarligNavEnhet = stringOrNull("arena_nav_enhet_enhetsnummer"),
            )
        },
        navn = string("navn"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.valueOf(string("status")),
        deltidsprosent = double("deltidsprosent"),
        antallPlasser = int("antall_plasser"),
        prismodell = toPrismodell(),
    )
}

private fun Row.toGjennomforingArena(): GjennomforingArena {
    return GjennomforingArena(
        id = uuid("id"),
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        opprettetTidspunkt = instant("opprettet_tidspunkt"),
        oppdatertTidspunkt = instant("oppdatert_tidspunkt"),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
        ),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        arena = stringOrNull("arena_tiltaksnummer")?.let {
            Gjennomforing.ArenaData(
                tiltaksnummer = Tiltaksnummer(it),
                ansvarligNavEnhet = stringOrNull("arena_nav_enhet_enhetsnummer"),
            )
        },
        navn = string("navn"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.valueOf(string("status")),
        deltidsprosent = double("deltidsprosent"),
        antallPlasser = int("antall_plasser"),
        oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
        pameldingType = GjennomforingPameldingType.valueOf(string("pamelding_type")),
    )
}
