package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.*
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
            insert into tiltaksgjennomforing (
                id,
                navn,
                tiltakstype_id,
                arrangor_id,
                start_dato,
                slutt_dato,
                avslutningsstatus,
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
                estimert_ventetid_enhet
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :arrangor_id,
                :start_dato,
                :slutt_dato,
                :avslutningsstatus::avslutningsstatus,
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
                :estimert_ventetid_enhet
            )
            on conflict (id)
                do update set navn                         = excluded.navn,
                              tiltakstype_id               = excluded.tiltakstype_id,
                              arrangor_id                  = excluded.arrangor_id,
                              start_dato                   = excluded.start_dato,
                              slutt_dato                   = excluded.slutt_dato,
                              avslutningsstatus            = excluded.avslutningsstatus,
                              apent_for_innsok             = excluded.apent_for_innsok,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = excluded.oppstart,
                              opphav                       = coalesce(tiltaksgjennomforing.opphav, excluded.opphav),
                              sted_for_gjennomforing       = excluded.sted_for_gjennomforing,
                              faneinnhold                  = excluded.faneinnhold,
                              beskrivelse                  = excluded.beskrivelse,
                              nav_region                   = excluded.nav_region,
                              deltidsprosent               = excluded.deltidsprosent,
                              estimert_ventetid_verdi      = excluded.estimert_ventetid_verdi,
                              estimert_ventetid_enhet      = excluded.estimert_ventetid_enhet
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
                avslutningsstatus,
                apent_for_innsok,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                deltidsprosent
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
                :avslutningsstatus::avslutningsstatus,
                :apent_for_innsok,
                :antall_plasser,
                :avtale_id,
                :oppstart::tiltaksgjennomforing_oppstartstype,
                :opphav::opphav,
                :deltidsprosent
            )
            on conflict (id)
                do update set navn                         = excluded.navn,
                              tiltakstype_id               = excluded.tiltakstype_id,
                              tiltaksnummer                = excluded.tiltaksnummer,
                              arrangor_id                  = excluded.arrangor_id,
                              arena_ansvarlig_enhet        = excluded.arena_ansvarlig_enhet,
                              start_dato                   = excluded.start_dato,
                              slutt_dato                   = excluded.slutt_dato,
                              avslutningsstatus            = excluded.avslutningsstatus,
                              apent_for_innsok             = excluded.apent_for_innsok,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = coalesce(tiltaksgjennomforing.oppstart, excluded.oppstart),
                              opphav                       = coalesce(tiltaksgjennomforing.opphav, excluded.opphav),
                              deltidsprosent               = excluded.deltidsprosent
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters(arrangorId)).asExecute.let { tx.run(it) }
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
        pagination: PaginationParams = PaginationParams(),
        search: String? = null,
        navEnheter: List<String> = emptyList(),
        tiltakstypeIder: List<UUID> = emptyList(),
        statuser: List<Tiltaksgjennomforingsstatus> = emptyList(),
        sortering: String? = null,
        sluttDatoCutoff: LocalDate? = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
        dagensDato: LocalDate = LocalDate.now(),
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<String> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        skalMigreres: Boolean? = null,
        opphav: ArenaMigrering.Opphav? = null,
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        val parameters = mapOf(
            "search" to search?.replace("/", "#")?.trim()?.let { "%$it%" },
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "slutt_dato_cutoff" to sluttDatoCutoff,
            "today" to dagensDato,
            "avtaleId" to avtaleId,
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { db.createTextArray(it) },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_orgnrs" to arrangorOrgnr.ifEmpty { null }?.let { db.createTextArray(it) },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "skal_migreres" to skalMigreres,
            "opphav" to opphav?.name,
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
            "publisert-ascending" -> "publisert_for_alle asc"
            "publisert-descending" -> "publisert_for_alle desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over () as full_count
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
              and (${statuserWhereStatement(statuser)})
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    private fun statuserWhereStatement(statuser: List<Tiltaksgjennomforingsstatus>): String =
        statuser
            .ifEmpty { null }
            ?.joinToString(prefix = "(", postfix = ")", separator = " or ") {
                when (it) {
                    PLANLAGT -> "(:today < start_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
                    GJENNOMFORES -> "((:today >= start_dato and (:today <= slutt_dato or slutt_dato is null)) and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
                    AVSLUTTET -> "(:today > slutt_dato or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
                    AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
                    AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVLYST}'"
                }
            }
            ?: "true"

    fun getAllVeilederflateTiltaksgjennomforing(
        search: String? = null,
        apentForInnsok: Boolean? = null,
        sanityTiltakstypeIds: List<UUID>? = null,
        innsatsgrupper: List<Innsatsgruppe> = emptyList(),
        brukersEnheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> {
        val parameters = mapOf(
            "search" to search?.let { "%${it.replace("/", "#").trim()}%" },
            "apent_for_innsok" to apentForInnsok,
            "sanityTiltakstypeIds" to sanityTiltakstypeIds?.let { db.createUuidArray(it) },
            "innsatsgrupper" to db.createTextArray(innsatsgrupper.map { it.name }),
            "brukersEnheter" to db.createTextArray(brukersEnheter),
        )

        @Language("PostgreSQL")
        val query = """
            select
                gjennomforing.id,
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
                tiltakstype.navn as tiltakstype_navn
            from tiltaksgjennomforing gjennomforing
                inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
                left join tiltaksgjennomforing_nav_enhet nav_enhet on nav_enhet.tiltaksgjennomforing_id = gjennomforing.id
                left join arrangor on arrangor.id = gjennomforing.arrangor_id
                left join tiltaksgjennomforing_arrangor_kontaktperson ak on ak.tiltaksgjennomforing_id = gjennomforing.id
                left join arrangor_kontaktperson on arrangor_kontaktperson.id = ak.arrangor_kontaktperson_id
            where tiltakstype.tiltakskode is not null
              and gjennomforing.publisert
              and gjennomforing.avslutningsstatus = 'IKKE_AVSLUTTET'
              and (:search::text is null or ((lower(gjennomforing.navn) like lower(:search)) or (gjennomforing.tiltaksnummer like :search)))
              and (:sanityTiltakstypeIds::uuid[] is null or tiltakstype.sanity_id = any(:sanityTiltakstypeIds))
              and (:innsatsgrupper::innsatsgruppe[] is null or tiltakstype.innsatsgruppe = any(:innsatsgrupper::innsatsgruppe[]))
              and (:apent_for_innsok::boolean is null or gjennomforing.apent_for_innsok = :apent_for_innsok)
            group by gjennomforing.id, tiltakstype.id, arrangor.id
            having array_agg(nav_enhet.enhetsnummer) && :brukersEnheter
        """.trimIndent()

        return queryOf(query, parameters)
            .map { it.toVeilederflateTiltaksgjennomforing() }
            .asList
            .let { db.run(it) }
    }

    fun getAllByDateIntervalAndAvslutningsstatus(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        avslutningsstatus: Avslutningsstatus,
        pagination: PaginationParams,
    ): List<UUID> {
        logger.info("Henter alle tiltaksgjennomføringer med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd, med avslutningsstatus $avslutningsstatus")

        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid
            from tiltaksgjennomforing tg
            where avslutningsstatus = :avslutningsstatus::avslutningsstatus and (
                (start_dato > :date_interval_start and start_dato <= :date_interval_end) or
                (slutt_dato >= :date_interval_start and slutt_dato < :date_interval_end))
            order by id
            limit :limit offset :offset
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "avslutningsstatus" to avslutningsstatus.name,
                "date_interval_start" to dateIntervalStart,
                "date_interval_end" to dateIntervalEnd,
                "limit" to pagination.limit,
                "offset" to pagination.offset,
            ),
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

    fun setAvtaleId(tx: Session, gjennomforingId: UUID, avtaleId: UUID?) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return queryOf(query, avtaleId, gjennomforingId).asUpdate.let { tx.run(it) }
    }

    fun setAvslutningsstatus(id: UUID, status: Avslutningsstatus): Int {
        return db.transaction { setAvslutningsstatus(it, id, status) }
    }

    fun getAvslutningsstatus(id: UUID): Avslutningsstatus {
        @Language("PostgreSQL")
        val query = """
            select avslutningsstatus from tiltaksgjennomforing where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { Avslutningsstatus.valueOf(it.string("avslutningsstatus")) }
            .asSingle
            .let { requireNotNull(db.run(it)) }
    }

    fun setAvslutningsstatus(tx: Session, id: UUID, status: Avslutningsstatus): Int {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avslutningsstatus = :status::avslutningsstatus
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "status" to status.name)).asUpdate)
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
        "avslutningsstatus" to Avslutningsstatus.IKKE_AVSLUTTET.name,
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
        "avslutningsstatus" to avslutningsstatus.name,
        "apent_for_innsok" to apentForInnsok,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "deltidsprosent" to deltidsprosent,
    )

    private fun Row.toVeilederflateTiltaksgjennomforing(): VeilederflateTiltaksgjennomforing {
        val navEnheter = arrayOrNull<String?>("nav_enheter")?.asList()?.filterNotNull() ?: emptyList()
        val arrangorKontaktpersoner = Json
            .decodeFromString<List<VeilederflateArrangorKontaktperson?>>(string("arrangor_kontaktpersoner_json"))
            .filterNotNull()

        return VeilederflateTiltaksgjennomforing(
            sanityId = uuidOrNull("sanity_id").toString(),
            id = uuidOrNull("id"),
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
        return TiltaksgjennomforingAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
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
            publisertForAlle = boolean("publisert_for_alle"),
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
            ),
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
}
