package no.nav.mulighetsrommet.api.repositories

import arrow.core.Either
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponseError
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.AvbruttDto
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatusDto
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo) =
        db.transaction { upsert(tiltaksgjennomforing, it) }

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo, tx: Session) {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")
        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id,
                                  navn,
                                  tiltakstype_id,
                                  arrangor_id,
                                  start_dato,
                                  slutt_dato,
                                  apent_for_innsok,
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
                                  tilgjengelig_for_arrangor_fra_og_med_dato,
                                  nusdata,
                                  amo_kategorisering
            )
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :arrangor_id,
                    :start_dato,
                    :slutt_dato,
                    :apent_for_innsok,
                    :antall_plasser,
                    :avtale_id,
                    :oppstart::tiltaksgjennomforing_oppstartstype,
                    :opphav::opphav,
                    :sted_for_gjennomforing,
                    :faneinnhold::jsonb,
                    :beskrivelse,
                    :nav_region,
                    :deltidsprosent,
                    :estimert_ventetid_verdi,
                    :estimert_ventetid_enhet,
                    :tilgjengelig_for_arrangor_fra_dato,
                    :nusdata::jsonb,
                    :amo_kategorisering::jsonb
            )
            on conflict (id)
                do update set navn                               = excluded.navn,
                              tiltakstype_id                     = excluded.tiltakstype_id,
                              arrangor_id                        = excluded.arrangor_id,
                              start_dato                         = excluded.start_dato,
                              slutt_dato                         = excluded.slutt_dato,
                              apent_for_innsok                   = excluded.apent_for_innsok,
                              antall_plasser                     = excluded.antall_plasser,
                              avtale_id                          = excluded.avtale_id,
                              oppstart                           = excluded.oppstart,
                              opphav                             = coalesce(tiltaksgjennomforing.opphav, excluded.opphav),
                              sted_for_gjennomforing             = excluded.sted_for_gjennomforing,
                              faneinnhold                        = excluded.faneinnhold,
                              beskrivelse                        = excluded.beskrivelse,
                              nav_region                         = excluded.nav_region,
                              deltidsprosent                     = excluded.deltidsprosent,
                              estimert_ventetid_verdi            = excluded.estimert_ventetid_verdi,
                              estimert_ventetid_enhet            = excluded.estimert_ventetid_enhet,
                              tilgjengelig_for_arrangor_fra_og_med_dato = excluded.tilgjengelig_for_arrangor_fra_og_med_dato,
                              nusdata                           = excluded.nusdata,
                              amo_kategorisering                = excluded.amo_kategorisering
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into tiltaksgjennomforing_nav_enhet (tiltaksgjennomforing_id, enhetsnummer)
             values (?::uuid, ?)
             on conflict (tiltaksgjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from tiltaksgjennomforing_nav_enhet
             where tiltaksgjennomforing_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertAdministrator = """
             insert into tiltaksgjennomforing_administrator (tiltaksgjennomforing_id, nav_ident)
             values (?::uuid, ?)
             on conflict (tiltaksgjennomforing_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from tiltaksgjennomforing_administrator
             where tiltaksgjennomforing_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertKontaktperson = """
            insert into tiltaksgjennomforing_kontaktperson (
                tiltaksgjennomforing_id,
                enheter,
                kontaktperson_nav_ident,
                beskrivelse
            )
            values (:id::uuid, :enheter, :nav_ident, :beskrivelse)
            on conflict (tiltaksgjennomforing_id, kontaktperson_nav_ident) do update set
                enheter = :enheter,
                beskrivelse = :beskrivelse
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from tiltaksgjennomforing_kontaktperson
            where tiltaksgjennomforing_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into tiltaksgjennomforing_arrangor_kontaktperson (
                arrangor_kontaktperson_id,
                tiltaksgjennomforing_id
            )
            values (:arrangor_kontaktperson_id::uuid, :tiltaksgjennomforing_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from tiltaksgjennomforing_arrangor_kontaktperson
            where tiltaksgjennomforing_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        tx.run(queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute)

        tiltaksgjennomforing.administratorer.forEach { administrator ->
            tx.run(
                queryOf(
                    upsertAdministrator,
                    tiltaksgjennomforing.id,
                    administrator.value,
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteAdministratorer,
                tiltaksgjennomforing.id,
                db.createTextArray(tiltaksgjennomforing.administratorer.map { it.value }),
            ).asExecute,
        )

        tiltaksgjennomforing.navEnheter.forEach { enhetId ->
            tx.run(
                queryOf(
                    upsertEnhet,
                    tiltaksgjennomforing.id,
                    enhetId,
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteEnheter,
                tiltaksgjennomforing.id,
                db.createTextArray(tiltaksgjennomforing.navEnheter),
            ).asExecute,
        )

        tiltaksgjennomforing.kontaktpersoner.forEach { kontakt ->
            tx.run(
                queryOf(
                    upsertKontaktperson,
                    mapOf(
                        "id" to tiltaksgjennomforing.id,
                        "enheter" to db.createTextArray(kontakt.navEnheter),
                        "nav_ident" to kontakt.navIdent.value,
                        "beskrivelse" to kontakt.beskrivelse,
                    ),
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteKontaktpersoner,
                tiltaksgjennomforing.id,
                tiltaksgjennomforing.kontaktpersoner.let { kontakt -> db.createTextArray(kontakt.map { it.navIdent.value }) },
            ).asExecute,
        )

        tiltaksgjennomforing.arrangorKontaktpersoner.forEach { person ->
            tx.run(
                queryOf(
                    upsertArrangorKontaktperson,
                    mapOf(
                        "tiltaksgjennomforing_id" to tiltaksgjennomforing.id,
                        "arrangor_kontaktperson_id" to person,
                    ),
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteArrangorKontaktpersoner,
                tiltaksgjennomforing.id,
                db.createUuidArray(tiltaksgjennomforing.arrangorKontaktpersoner),
            ).asExecute,
        )
    }

    fun upsertArenaTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo) {
        db.transaction { upsertArenaTiltaksgjennomforing(tiltaksgjennomforing, it) }
    }

    fun upsertArenaTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo, tx: Session) {
        logger.info("Lagrer tiltaksgjennomføring fra Arena id=${tiltaksgjennomforing.id}")

        val arrangorId = queryOf(
            "select id from arrangor where organisasjonsnummer = ?",
            tiltaksgjennomforing.arrangorOrganisasjonsnummer,
        )
            .map { it.uuid("id") }
            .asSingle
            .let { requireNotNull(db.run(it)) }

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (
                id,
                navn,
                tiltakstype_id,
                tiltaksnummer,
                arrangor_id,
                arena_ansvarlig_enhet,
                start_dato,
                slutt_dato,
                apent_for_innsok,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                deltidsprosent,
                avbrutt_tidspunkt,
                avbrutt_aarsak
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :tiltaksnummer,
                :arrangor_id,
                :arena_ansvarlig_enhet,
                :start_dato,
                :slutt_dato,
                :apent_for_innsok,
                :antall_plasser,
                :avtale_id,
                (select case
                     when arena_kode in ('GRUPPEAMO', 'JOBBK', 'GRUFAGYRKE') then 'FELLES'
                     else 'LOPENDE'
                     end::tiltaksgjennomforing_oppstartstype
                 from tiltakstype
                 where tiltakstype.id = :tiltakstype_id::uuid),
                :opphav::opphav,
                :deltidsprosent,
                :avbrutt_tidspunkt,
                :avbrutt_aarsak
            )
            on conflict (id)
                do update set navn                         = excluded.navn,
                              tiltakstype_id               = excluded.tiltakstype_id,
                              tiltaksnummer                = excluded.tiltaksnummer,
                              arrangor_id                  = excluded.arrangor_id,
                              arena_ansvarlig_enhet        = excluded.arena_ansvarlig_enhet,
                              start_dato                   = excluded.start_dato,
                              slutt_dato                   = excluded.slutt_dato,
                              apent_for_innsok             = excluded.apent_for_innsok,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = coalesce(tiltaksgjennomforing.oppstart, excluded.oppstart),
                              opphav                       = coalesce(tiltaksgjennomforing.opphav, excluded.opphav),
                              deltidsprosent               = excluded.deltidsprosent,
                              avbrutt_tidspunkt            = excluded.avbrutt_tidspunkt,
                              avbrutt_aarsak               = excluded.avbrutt_aarsak
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters(arrangorId)).asExecute.let { tx.run(it) }
    }

    fun updateArenaData(id: UUID, tiltaksnummer: String, arenaAnsvarligEnhet: String?, tx: Session) {
        logger.info("Oppdaterer tiltaksgjennomføring med Arena data id=$id")

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set
                tiltaksnummer = :tiltaksnummer, arena_ansvarlig_enhet = :arena_ansvarlig_enhet
            where id = :id::uuid
        """.trimIndent()

        queryOf(
            query,
            mapOf("id" to id, "tiltaksnummer" to tiltaksnummer, "arena_ansvarlig_enhet" to arenaAnsvarligEnhet),
        )
            .asExecute.let { tx.run(it) }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? =
        db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): TiltaksgjennomforingAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltaksgjennomforing_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asSingle
            .let { tx.run(it) }
    }

    fun getUpdatedAt(id: UUID): LocalDateTime? {
        @Language("PostgreSQL")
        val query = """
            select updated_at from tiltaksgjennomforing where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.localDateTimeOrNull("updated_at") }
            .asSingle
            .let { db.run(it) }
    }

    fun getSanityTiltaksgjennomforingId(id: UUID, tx: Session): UUID? {
        @Language("PostgreSQL")
        val query = """
            select sanity_id
            from tiltaksgjennomforing
            where id = :id::uuid
        """.trimIndent()

        return queryOf(query, mapOf("id" to id))
            .map { it.uuidOrNull("sanity_id") }
            .asSingle
            .runWithSession(tx)
    }

    fun updateSanityTiltaksgjennomforingId(id: UUID, sanityId: UUID) =
        db.transaction { updateSanityTiltaksgjennomforingId(id, sanityId, it) }

    fun updateSanityTiltaksgjennomforingId(id: UUID, sanityId: UUID, tx: Session) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
                set sanity_id = :sanity_id::uuid
                where id = :id::uuid
                and sanity_id is null
        """.trimIndent()

        queryOf(
            query,
            mapOf(
                "sanity_id" to sanityId,
                "id" to id,
            ),
        )
            .asUpdate
            .let { tx.run(it) }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        search: String? = null,
        navEnheter: List<String> = emptyList(),
        tiltakstypeIder: List<UUID> = emptyList(),
        statuser: List<TiltaksgjennomforingStatus> = emptyList(),
        sortering: String? = null,
        sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<String> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        skalMigreres: Boolean? = null,
        opphav: ArenaMigrering.Opphav? = null,
        publisert: Boolean? = null,
    ): PaginatedResult<TiltaksgjennomforingAdminDto> {
        val parameters = mapOf(
            "search" to search?.replace("/", "#")?.trim()?.let { "%$it%" },
            "slutt_dato_cutoff" to sluttDatoGreaterThanOrEqualTo,
            "avtale_id" to avtaleId,
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { db.createTextArray(it) },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_orgnrs" to arrangorOrgnr.ifEmpty { null }?.let { db.createTextArray(it) },
            "statuser" to statuser.ifEmpty { null }?.let { db.createArrayOf("text", statuser) },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "skal_migreres" to skalMigreres,
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
            from tiltaksgjennomforing_admin_dto_view
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
              and (:avtale_id::uuid is null or avtale_id = :avtale_id)
              and (:arrangor_ids::uuid[] is null or arrangor_id = any(:arrangor_ids))
              and (:arrangor_orgnrs::text[] is null or arrangor_organisasjonsnummer = any(:arrangor_orgnrs))
              and (:search::text is null or (navn ilike :search or tiltaksnummer ilike :search or arrangor_navn ilike :search))
              and (:nav_enheter::text[] is null or (
                   nav_region_enhetsnummer = any (:nav_enheter) or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter)))
              and (:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb)
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and (:skal_migreres::boolean is null or tiltakstype_tiltakskode is not null)
              and (:opphav::opphav is null or opphav = :opphav::opphav)
              and (:statuser::text[] is null or status = any(:statuser))
              and (:publisert::boolean is null or publisert = :publisert::boolean)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return db.useSession { session ->
            queryOf(query, parameters + pagination.parameters)
                .mapPaginated { it.toTiltaksgjennomforingAdminDto() }
                .runWithSession(session)
        }
    }

    fun getAllVeilederflateTiltaksgjennomforing(
        search: String? = null,
        apentForInnsok: Boolean? = null,
        sanityTiltakstypeIds: List<UUID>? = null,
        innsatsgruppe: Innsatsgruppe? = null,
        brukersEnheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> {
        val parameters = mapOf(
            "search" to search?.let { "%${it.replace("/", "#").trim()}%" },
            "apent_for_innsok" to apentForInnsok,
            "sanityTiltakstypeIds" to sanityTiltakstypeIds?.let { db.createUuidArray(it) },
            "innsatsgruppe" to innsatsgruppe?.name,
            "brukersEnheter" to db.createTextArray(brukersEnheter),
        )

        @Language("PostgreSQL")
        val query = """
            select
                gjennomforing.id,
                a.id as avtale_id,
                gjennomforing.sanity_id,
                gjennomforing.navn,
                gjennomforing.sted_for_gjennomforing,
                gjennomforing.apent_for_innsok,
                gjennomforing.tiltaksnummer,
                gjennomforing.oppstart,
                gjennomforing.start_dato,
                gjennomforing.slutt_dato,
                gjennomforing.estimert_ventetid_verdi,
                gjennomforing.estimert_ventetid_enhet,
                gjennomforing.beskrivelse,
                gjennomforing.faneinnhold,
                gjennomforing.nav_region,
                array_agg(nav_enhet.enhetsnummer) as nav_enheter,
                arrangor.id as arrangor_id,
                arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
                arrangor.navn                 as arrangor_navn,
                jsonb_agg(distinct
                    case when ak.tiltaksgjennomforing_id is null then null::jsonb
                    else jsonb_build_object(
                        'id', arrangor_kontaktperson.id,
                        'navn', arrangor_kontaktperson.navn,
                        'telefon', arrangor_kontaktperson.telefon,
                        'epost', arrangor_kontaktperson.epost,
                        'beskrivelse', arrangor_kontaktperson.beskrivelse
                    ) end
                ) as arrangor_kontaktpersoner_json,
                tiltakstype.sanity_id as tiltakstype_sanity_id,
                tiltakstype.navn as tiltakstype_navn,
                a.personvern_bekreftet,
                gjennomforing.avbrutt_tidspunkt,
                tiltaksgjennomforing_status(gjennomforing.start_dato, gjennomforing.slutt_dato, gjennomforing.avbrutt_tidspunkt) as status,
                gjennomforing.avbrutt_tidspunkt,
                gjennomforing.avbrutt_aarsak
            from tiltaksgjennomforing gjennomforing
                inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
                left join tiltaksgjennomforing_nav_enhet nav_enhet on nav_enhet.tiltaksgjennomforing_id = gjennomforing.id
                left join arrangor on arrangor.id = gjennomforing.arrangor_id
                left join tiltaksgjennomforing_arrangor_kontaktperson ak on ak.tiltaksgjennomforing_id = gjennomforing.id
                left join arrangor_kontaktperson on arrangor_kontaktperson.id = ak.arrangor_kontaktperson_id
                left join avtale a on a.id = gjennomforing.avtale_id
            where tiltakstype.tiltakskode is not null
              and gjennomforing.publisert
              and (:search::text is null or ((lower(gjennomforing.navn) like lower(:search)) or (gjennomforing.tiltaksnummer like :search)))
              and (:sanityTiltakstypeIds::uuid[] is null or tiltakstype.sanity_id = any(:sanityTiltakstypeIds))
              and (:innsatsgruppe::innsatsgruppe is null or :innsatsgruppe::innsatsgruppe = any(tiltakstype.innsatsgrupper))
              and (:apent_for_innsok::boolean is null or gjennomforing.apent_for_innsok = :apent_for_innsok)
            group by gjennomforing.id, tiltakstype.id, arrangor.id, a.id
            having array_agg(nav_enhet.enhetsnummer) && :brukersEnheter
        """.trimIndent()

        return queryOf(query, parameters)
            .map { it.toVeilederflateTiltaksgjennomforing() }
            .asList
            .let { db.run(it) }
    }

    fun getAllByDateIntervalAndNotAvbrutt(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        pagination: Pagination,
    ): List<UUID> {
        logger.info("Henter alle tiltaksgjennomføringer med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd, og ikke avbrutt")

        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid
            from tiltaksgjennomforing tg
            where avbrutt_tidspunkt is null and (
                (start_dato > :date_interval_start and start_dato <= :date_interval_end) or
                (slutt_dato >= :date_interval_start and slutt_dato < :date_interval_end))
            order by id
            limit :limit offset :offset
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "date_interval_start" to dateIntervalStart,
                "date_interval_end" to dateIntervalEnd,
            ) + pagination.parameters,
        )
            .map { it.uuid("id") }
            .asList
            .let { db.run(it) }
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(currentDate: LocalDate = LocalDate.now()): List<TiltaksgjennomforingNotificationDto> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.start_dato,
                   tg.slutt_dato,
                   array_agg(distinct a.nav_ident) as administratorer,
                   array_agg(e.enhetsnummer) as navEnheter,
                   tg.tiltaksnummer
            from tiltaksgjennomforing tg
                     left join tiltaksgjennomforing_administrator a on a.tiltaksgjennomforing_id = tg.id
                    left join tiltaksgjennomforing_nav_enhet e on e.tiltaksgjennomforing_id = tg.id
            where (?::timestamp + interval '14' day) = tg.slutt_dato
               or (?::timestamp + interval '7' day) = tg.slutt_dato
               or (?::timestamp + interval '1' day) = tg.slutt_dato
            group by tg.id;
        """.trimIndent()

        return queryOf(query, currentDate, currentDate, currentDate).map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID): Int =
        db.transaction { delete(id, it) }

    fun delete(id: UUID, tx: Session): Int {
        logger.info("Sletter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        return tx.run(queryOf(query, id).asUpdate)
    }

    fun setOpphav(id: UUID, opphav: ArenaMigrering.Opphav) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set opphav = :opphav::opphav
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "opphav" to opphav.name))
            .asUpdate
            .let { db.run(it) }
    }

    fun setPublisert(id: UUID, publisert: Boolean): Int {
        return db.transaction { setPublisert(it, id, publisert) }
    }

    fun setPublisert(tx: Session, id: UUID, publisert: Boolean): Int {
        logger.info("Setter publisert '$publisert' for gjennomføring med id: $id")
        @Language("PostgreSQL")
        val query = """
           update tiltaksgjennomforing
           set publisert = ?
           where id = ?::uuid
        """.trimIndent()

        return queryOf(query, publisert, id).asUpdate.let { tx.run(it) }
    }

    fun setTilgjengeligForArrangorFraOgMedDato(tx: TransactionalSession, id: UUID, date: LocalDate) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set tilgjengelig_for_arrangor_fra_og_med_dato = :date
            where id = :id
        """.trimIndent()

        val parameters = mapOf(
            "id" to id,
            "date" to date,
        )

        tx.run(queryOf(query, parameters).asUpdate)
    }

    fun setAvtaleId(tx: Session, gjennomforingId: UUID, avtaleId: UUID?) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return queryOf(query, avtaleId, gjennomforingId).asUpdate.let { tx.run(it) }
    }

    fun avbryt(id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak): Int {
        return db.transaction { avbryt(it, id, tidspunkt, aarsak) }
    }

    fun avbryt(tx: Session, id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak): Int {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set
                avbrutt_tidspunkt = :tidspunkt,
                avbrutt_aarsak = :aarsak,
                publisert = false
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt, "aarsak" to aarsak.name)).asUpdate)
    }

    fun lukkApentForInnsokForTiltakMedStartdatoForDato(
        dagensDato: LocalDate,
        tx: TransactionalSession,
    ): List<TiltaksgjennomforingAdminDto> {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set apent_for_innsok = false
            where apent_for_innsok = true and oppstart = 'FELLES' and start_dato = ? and opphav = 'MR_ADMIN_FLATE'
            returning id
        """.trimIndent()

        return queryOf(query, dagensDato).map { get(it.uuid("id")) }.asList.let { tx.run(it) }
    }

    private fun TiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.MR_ADMIN_FLATE.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "arrangor_id" to arrangorId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "apent_for_innsok" to apentForInnsok,
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
        "nusdata" to nusData?.let { Json.encodeToString(it) },
        "amo_kategorisering" to amoKategorisering?.let { Json.encodeToString(it) },
    )

    private fun ArenaTiltaksgjennomforingDbo.toSqlParameters(arrangorId: UUID) = mapOf(
        "opphav" to ArenaMigrering.Opphav.ARENA.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "arrangor_id" to arrangorId,
        "start_dato" to startDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        "slutt_dato" to sluttDato,
        "apent_for_innsok" to apentForInnsok,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "deltidsprosent" to deltidsprosent,
        "avbrutt_tidspunkt" to when (avslutningsstatus) {
            Avslutningsstatus.AVLYST -> startDato.atStartOfDay().minusDays(1)
            Avslutningsstatus.AVBRUTT -> startDato.atStartOfDay()
            Avslutningsstatus.AVSLUTTET -> null
            Avslutningsstatus.IKKE_AVSLUTTET -> null
        },
        "avbrutt_aarsak" to when (avslutningsstatus) {
            Avslutningsstatus.AVLYST, Avslutningsstatus.AVBRUTT -> AvbruttAarsak.AvbruttIArena.name
            Avslutningsstatus.AVSLUTTET, Avslutningsstatus.IKKE_AVSLUTTET -> null
        },
    )

    private fun Row.toVeilederflateTiltaksgjennomforing(): VeilederflateTiltaksgjennomforing {
        val navEnheter = arrayOrNull<String?>("nav_enheter")?.asList()?.filterNotNull() ?: emptyList()
        val arrangorKontaktpersoner = Json
            .decodeFromString<List<VeilederflateArrangorKontaktperson?>>(string("arrangor_kontaktpersoner_json"))
            .filterNotNull()

        val avbruttTidspunkt = localDateTimeOrNull("avbrutt_tidspunkt")
        val avbruttAarsak = stringOrNull("avbrutt_aarsak")?.let { AvbruttAarsak.fromString(it) }

        return VeilederflateTiltaksgjennomforing(
            sanityId = uuidOrNull("sanity_id").toString(),
            id = uuidOrNull("id"),
            avtaleId = uuidOrNull("avtale_id"),
            tiltakstype = VeilederflateTiltakstype(
                sanityId = uuid("tiltakstype_sanity_id").toString(),
                navn = string("tiltakstype_navn"),
            ),
            navn = string("navn"),
            stedForGjennomforing = stringOrNull("sted_for_gjennomforing"),
            apentForInnsok = boolean("apent_for_innsok"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            oppstart = TiltaksgjennomforingOppstartstype.valueOf(string("oppstart")),
            oppstartsdato = localDate("start_dato"),
            sluttdato = localDateOrNull("slutt_dato"),
            arrangor = VeilederflateArrangor(
                arrangorId = uuid("arrangor_id"),
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                selskapsnavn = stringOrNull("arrangor_navn"),
                kontaktpersoner = arrangorKontaktpersoner,
            ),
            fylke = stringOrNull("nav_region"),
            enheter = navEnheter,
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
                EstimertVentetid(
                    verdi = int("estimert_ventetid_verdi"),
                    enhet = string("estimert_ventetid_enhet"),
                )
            },
            personvernBekreftet = boolean("personvern_bekreftet"),
            status = TiltaksgjennomforingStatusDto(
                TiltaksgjennomforingStatus.valueOf(string("status")),
                avbruttTidspunkt?.let {
                    requireNotNull(avbruttAarsak)
                    AvbruttDto(
                        tidspunkt = avbruttTidspunkt,
                        aarsak = avbruttAarsak,
                        beskrivelse = avbruttAarsak.beskrivelse,
                    )
                },
            ),
        )
    }

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val administratorer = Json
            .decodeFromString<List<TiltaksgjennomforingAdminDto.Administrator?>>(string("administratorer_json"))
            .filterNotNull()
        val navEnheterDto = Json.decodeFromString<List<NavEnhetDbo?>>(string("nav_enheter_json")).filterNotNull()
        val kontaktpersoner = Json
            .decodeFromString<List<TiltaksgjennomforingKontaktperson?>>(string("nav_kontaktpersoner_json"))
            .filterNotNull()
        val arrangorKontaktpersoner = Json
            .decodeFromString<List<ArrangorKontaktperson?>>(string("arrangor_kontaktpersoner_json"))
            .filterNotNull()

        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")

        val avbruttTidspunkt = localDateTimeOrNull("avbrutt_tidspunkt")
        val avbruttAarsak = stringOrNull("avbrutt_aarsak")?.let { AvbruttAarsak.fromString(it) }

        return TiltaksgjennomforingAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            status = TiltaksgjennomforingStatusDto(
                TiltaksgjennomforingStatus.valueOf(string("status")),
                avbruttTidspunkt?.let {
                    requireNotNull(avbruttAarsak)
                    AvbruttDto(
                        tidspunkt = avbruttTidspunkt,
                        aarsak = avbruttAarsak,
                        beskrivelse = avbruttAarsak.beskrivelse,
                    )
                },
            ),
            apentForInnsok = boolean("apent_for_innsok"),
            sanityId = uuidOrNull("sanity_id"),
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            oppstart = TiltaksgjennomforingOppstartstype.valueOf(string("oppstart")),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            createdAt = localDateTime("created_at"),
            deltidsprosent = double("deltidsprosent"),
            estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
                TiltaksgjennomforingAdminDto.EstimertVentetid(
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
            arrangor = TiltaksgjennomforingAdminDto.ArrangorUnderenhet(
                id = uuid("arrangor_id"),
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
                kontaktpersoner = arrangorKontaktpersoner,
            ),
            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakstype_arena_kode"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
            ),
            personvernBekreftet = boolean("personvern_bekreftet"),
            tilgjengeligForArrangorFraOgMedDato = localDateOrNull("tilgjengelig_for_arrangor_fra_og_med_dato"),
            nusData = stringOrNull("nusdata")?.let { Json.decodeFromString(it) },
            amoKategorisering = stringOrNull("amo_kategorisering")?.let { Json.decodeFromString(it) },
        )
    }

    private fun Row.toTiltaksgjennomforingNotificationDto(): TiltaksgjennomforingNotificationDto {
        val administratorer = arrayOrNull<String?>("administratorer")
            ?.asList()
            ?.filterNotNull()
            ?.map { NavIdent(it) }
            ?: emptyList()

        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        return TiltaksgjennomforingNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            startDato = startDato,
            sluttDato = sluttDato,
            administratorer = administratorer,
            tiltaksnummer = stringOrNull("tiltaksnummer"),
        )
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        tx: Session,
    ): Either<StatusResponseError, String> {
        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and tiltaksgjennomforing_id = ?::uuid
        """.trimIndent()

        queryOf(query, kontaktpersonId, gjennomforingId)
            .asUpdate
            .let { tx.run(it) }

        return Either.Right(kontaktpersonId.toString())
    }
}
