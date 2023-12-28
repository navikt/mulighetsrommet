package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
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
                tiltaksnummer,
                arrangor_organisasjonsnummer,
                arrangor_kontaktperson_id,
                start_dato,
                slutt_dato,
                avslutningsstatus,
                apent_for_innsok,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                stengt_fra,
                stengt_til,
                sted_for_gjennomforing,
                faneinnhold,
                beskrivelse,
                nav_region,
                fremmote_tidspunkt,
                fremmote_sted
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :tiltaksnummer,
                :arrangor_organisasjonsnummer,
                :arrangor_kontaktperson_id,
                :start_dato,
                :slutt_dato,
                :avslutningsstatus::avslutningsstatus,
                :apent_for_innsok,
                :antall_plasser,
                :avtale_id,
                :oppstart::tiltaksgjennomforing_oppstartstype,
                :opphav::opphav,
                :stengt_fra,
                :stengt_til,
                :sted_for_gjennomforing,
                :faneinnhold::jsonb,
                :beskrivelse,
                :nav_region,
                :fremmote_tidspunkt,
                :fremmote_sted
            )
            on conflict (id)
                do update set navn                         = excluded.navn,
                              tiltakstype_id               = excluded.tiltakstype_id,
                              tiltaksnummer                = excluded.tiltaksnummer,
                              arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                              arrangor_kontaktperson_id    = excluded.arrangor_kontaktperson_id,
                              start_dato                   = excluded.start_dato,
                              slutt_dato                   = excluded.slutt_dato,
                              avslutningsstatus            = excluded.avslutningsstatus,
                              apent_for_innsok             = excluded.apent_for_innsok,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = excluded.oppstart,
                              opphav                       = excluded.opphav,
                              stengt_fra                   = excluded.stengt_fra,
                              stengt_til                   = excluded.stengt_til,
                              sted_for_gjennomforing       = excluded.sted_for_gjennomforing,
                              faneinnhold                  = excluded.faneinnhold,
                              beskrivelse                  = excluded.beskrivelse,
                              nav_region                   = excluded.nav_region,
                              fremmote_tidspunkt           = excluded.fremmote_tidspunkt,
                              fremmote_sted                = excluded.fremmote_sted
            returning *
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
            insert into tiltaksgjennomforing_kontaktperson (tiltaksgjennomforing_id, enheter, kontaktperson_nav_ident)
            values (?::uuid, ?, ?)
            on conflict (tiltaksgjennomforing_id, kontaktperson_nav_ident) do update set enheter = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from tiltaksgjennomforing_kontaktperson
            where tiltaksgjennomforing_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()

        tx.run(queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute)

        tiltaksgjennomforing.administratorer.forEach { administrator ->
            tx.run(
                queryOf(
                    upsertAdministrator,
                    tiltaksgjennomforing.id,
                    administrator,
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteAdministratorer,
                tiltaksgjennomforing.id,
                db.createTextArray(tiltaksgjennomforing.administratorer),
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
                    tiltaksgjennomforing.id,
                    db.createTextArray(kontakt.navEnheter),
                    kontakt.navIdent,
                    db.createTextArray(kontakt.navEnheter),
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteKontaktpersoner,
                tiltaksgjennomforing.id,
                tiltaksgjennomforing.kontaktpersoner.let { kontakt -> db.createTextArray(kontakt.map { it.navIdent }) },
            ).asExecute,
        )
    }

    fun upsertArenaTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo) {
        db.transaction { upsertArenaTiltaksgjennomforing(tiltaksgjennomforing, it) }
    }

    fun upsertArenaTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo, tx: Session) {
        logger.info("Lagrer tiltaksgjennomføring fra Arena id=${tiltaksgjennomforing.id}")
        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (
                id,
                navn,
                tiltakstype_id,
                tiltaksnummer,
                arrangor_organisasjonsnummer,
                arena_ansvarlig_enhet,
                start_dato,
                slutt_dato,
                avslutningsstatus,
                apent_for_innsok,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                fremmote_tidspunkt,
                fremmote_sted
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :tiltaksnummer,
                :arrangor_organisasjonsnummer,
                :arena_ansvarlig_enhet,
                :start_dato,
                :slutt_dato,
                :avslutningsstatus::avslutningsstatus,
                :apent_for_innsok,
                :antall_plasser,
                :avtale_id,
                :oppstart::tiltaksgjennomforing_oppstartstype,
                :opphav::opphav,
                :fremmote_tidspunkt,
                :fremmote_sted
            )
            on conflict (id)
                do update set navn                         = excluded.navn,
                              tiltakstype_id               = excluded.tiltakstype_id,
                              tiltaksnummer                = excluded.tiltaksnummer,
                              arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                              arena_ansvarlig_enhet        = excluded.arena_ansvarlig_enhet,
                              start_dato                   = excluded.start_dato,
                              slutt_dato                   = excluded.slutt_dato,
                              avslutningsstatus            = excluded.avslutningsstatus,
                              apent_for_innsok             = excluded.apent_for_innsok,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = coalesce(tiltaksgjennomforing.oppstart, excluded.oppstart),
                              opphav                       = excluded.opphav,
                              fremmote_tidspunkt           = excluded.fremmote_tidspunkt,
                              fremmote_sted                = excluded.fremmote_sted
            returning *
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute.let { tx.run(it) }
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
        navRegioner: List<String> = emptyList(),
        avtaleId: UUID? = null,
        arrangorOrgnr: List<String> = emptyList(),
        administratorNavIdent: String? = null,
        skalMigreres: Boolean? = null,
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        val parameters = mapOf(
            "search" to "%${search?.replace("/", "#")?.trim()}%",
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "cutoffdato" to sluttDatoCutoff,
            "today" to dagensDato,
            "avtaleId" to avtaleId,
            "arrangor_organisasjonsnummer" to arrangorOrgnr,
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "$it" }]""" },
            "skalMigreres" to skalMigreres,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            search to "((lower(navn) like lower(:search)) or (tiltaksnummer like :search))",
            navEnheter.ifEmpty { null } to navEnheterWhereStatement(navEnheter),
            tiltakstypeIder.ifEmpty { null } to tiltakstypeIderWhereStatement(tiltakstypeIder),
            statuser.ifEmpty { null } to statuserWhereStatement(statuser),
            sluttDatoCutoff to "(slutt_dato >= :cutoffdato or slutt_dato is null)",
            navRegioner.ifEmpty { null } to navRegionerWhereStatement(navRegioner),
            avtaleId to "avtale_id = :avtaleId",
            arrangorOrgnr.ifEmpty { null } to arrangorOrganisasjonsnummerWhereStatement(arrangorOrgnr),
            administratorNavIdent to "administratorer @> :administrator_nav_ident::jsonb",
            skalMigreres to "skal_migreres = :skalMigreres",
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
            "vises_for_veileder-ascending" -> "vises_for_veileder asc"
            "vises_for_veileder-descending" -> "vises_for_veileder desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over () as full_count
            from tiltaksgjennomforing_admin_dto_view
            $where
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

    private fun navEnheterWhereStatement(navEnheter: List<String>): String =
        navEnheter
            .joinToString(prefix = "(", postfix = ")", separator = " or ") {
                "('$it' in (select enhetsnummer from tiltaksgjennomforing_nav_enhet tg_e where tg_e.tiltaksgjennomforing_id = id) or arena_ansvarlig_enhet::jsonb->>'enhetsnummer' = '$it')"
            }

    private fun tiltakstypeIderWhereStatement(tiltakstypeIder: List<UUID>): String =
        tiltakstypeIder
            .joinToString(prefix = "(", postfix = ")", separator = " or ") {
                "tiltakstype_id = '$it'"
            }

    private fun navRegionerWhereStatement(navRegioner: List<String>): String =
        navRegioner
            .joinToString(prefix = "(", postfix = ")", separator = " or ") {
                "('$it' = nav_region_enhetsnummer or  arena_ansvarlig_enhet::jsonb->>'enhetsnummer' = '$it' or arena_ansvarlig_enhet::jsonb->>'enhetsnummer' in (select enhetsnummer from nav_enhet where overordnet_enhet = '$it'))"
            }

    private fun statuserWhereStatement(statuser: List<Tiltaksgjennomforingsstatus>): String =
        statuser
            .joinToString(prefix = "(", postfix = ")", separator = " or ") {
                when (it) {
                    PLANLAGT -> "(:today < start_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
                    GJENNOMFORES -> "((:today >= start_dato and (:today <= slutt_dato or slutt_dato is null)) and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
                    AVSLUTTET -> "(:today > slutt_dato or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
                    AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
                    AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVLYST}'"
                }
            }

    private fun arrangorOrganisasjonsnummerWhereStatement(arrangorOrgnr: List<String>): String =
        arrangorOrgnr
            .joinToString(prefix = "(", postfix = ")", separator = " or ") {
                "arrangor_organisasjonsnummer = '$it'"
            }

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

        val where = DatabaseUtils.andWhereParameterNotNull(
            search to "((lower(tg.navn) like lower(:search)) or (tg.tiltaksnummer like :search))",
            sanityTiltakstypeIds to "t.sanity_id = any(:sanityTiltakstypeIds)",
            innsatsgrupper to "t.innsatsgruppe = any(:innsatsgrupper::innsatsgruppe[])",
            apentForInnsok to "tg.apent_for_innsok = :apent_for_innsok",
        )

        @Language("PostgreSQL")
        val query = """
            select
                   tg.id,
                   tg.sanity_id,
                   t.sanity_id as tiltakstype_sanity_id,
                   t.navn as tiltakstype_navn,
                   tg.navn,
                   tg.sted_for_gjennomforing,
                   tg.apent_for_innsok,
                   tg.tiltaksnummer,
                   tg.oppstart,
                   tg.start_dato,
                   tg.slutt_dato,
                   tg.arrangor_organisasjonsnummer,
                   v.navn                 as arrangor_navn,
                   tg.arrangor_kontaktperson_id,
                   vk.navn                as arrangor_kontaktperson_navn,
                   vk.telefon             as arrangor_kontaktperson_telefon,
                   vk.epost               as arrangor_kontaktperson_epost,
                   tg.stengt_fra,
                   tg.stengt_til,
                   jsonb_agg(distinct
                             case
                                 when tgk.tiltaksgjennomforing_id is null then null::jsonb
                                 else jsonb_build_object('navn', concat(na.fornavn, ' ', na.etternavn), 'epost', na.epost, 'telefonnummer',na.mobilnummer)
                                 end
                       )                  as kontaktpersoner,
                   tg.nav_region,
                   array_agg(tg_e.enhetsnummer) as nav_enheter,
                   tg.beskrivelse,
                   tg.faneinnhold
            from tiltaksgjennomforing tg
                     inner join tiltakstype t on tg.tiltakstype_id = t.id
                     left join tiltaksgjennomforing_nav_enhet tg_e on tg_e.tiltaksgjennomforing_id = tg.id
                     left join virksomhet v on v.organisasjonsnummer = tg.arrangor_organisasjonsnummer
                     left join tiltaksgjennomforing_kontaktperson tgk on tgk.tiltaksgjennomforing_id = tg.id
                     left join nav_ansatt na on na.nav_ident = tgk.kontaktperson_nav_ident
                     left join virksomhet_kontaktperson vk on vk.id = tg.arrangor_kontaktperson_id
            $where
            and t.skal_migreres
            and tg.tilgjengelig_for_veileder
            and tg.avslutningsstatus = 'IKKE_AVSLUTTET'
            group by tg.id, t.id, v.navn, vk.id
            having array_agg(tg_e.enhetsnummer) && :brukersEnheter
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

    fun setTilgjengeligForVeileder(id: UUID, tilgjengeligForVeileder: Boolean): Int {
        return db.transaction { setTilgjengeligForVeileder(it, id, tilgjengeligForVeileder) }
    }

    fun setTilgjengeligForVeileder(tx: Session, id: UUID, tilgjengeligForVeileder: Boolean): Int {
        logger.info("Setter tilgjengelig for veileder '$tilgjengeligForVeileder' for gjennomføring med id: $id")
        @Language("PostgreSQL")
        val query = """
           update tiltaksgjennomforing
           set tilgjengelig_for_veileder = ?
           where id = ?::uuid
        """.trimIndent()

        return queryOf(query, tilgjengeligForVeileder, id).asUpdate.let { tx.run(it) }
    }

    private fun TiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer,
        "arrangor_kontaktperson_id" to arrangorKontaktpersonId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "avslutningsstatus" to avslutningsstatus.name,
        "apent_for_innsok" to apentForInnsok,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "opphav" to opphav.name,
        "stengt_fra" to stengtFra,
        "stengt_til" to stengtTil,
        "sted_for_gjennomforing" to stedForGjennomforing,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
        "beskrivelse" to beskrivelse,
        "nav_region" to navRegion,
        "fremmote_tidspunkt" to fremmoteTidspunkt,
        "fremmote_sted" to fremmoteSted,
    )

    private fun ArenaTiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer,
        "start_dato" to startDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        "slutt_dato" to sluttDato,
        "avslutningsstatus" to avslutningsstatus.name,
        "apent_for_innsok" to apentForInnsok,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "opphav" to opphav.name,
        "fremmote_tidspunkt" to fremmoteTidspunkt,
        "fremmote_sted" to fremmoteSted,
    )

    private fun Row.toVeilederflateTiltaksgjennomforing(): VeilederflateTiltaksgjennomforing {
        val navEnheter = arrayOrNull<String?>("nav_enheter")?.asList()?.filterNotNull() ?: emptyList()
        val kontaktpersoner = Json
            .decodeFromString<List<KontaktinfoTiltaksansvarlige?>>(string("kontaktpersoner"))
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
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                selskapsnavn = stringOrNull("arrangor_navn"),
                kontaktperson = uuidOrNull("arrangor_kontaktperson_id")?.let {
                    VeilederflateArrangor.Kontaktperson(
                        navn = string("arrangor_kontaktperson_navn"),
                        telefon = stringOrNull("arrangor_kontaktperson_telefon"),
                        epost = string("arrangor_kontaktperson_epost"),
                    )
                },
            ),
            stengtFra = localDateOrNull("stengt_fra"),
            stengtTil = localDateOrNull("stengt_til"),
            kontaktinfoTiltaksansvarlige = kontaktpersoner,
            fylke = stringOrNull("nav_region"),
            enheter = navEnheter,
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        )
    }

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val administratorer = Json
            .decodeFromString<List<TiltaksgjennomforingAdminDto.Administrator?>>(string("administratorer"))
            .filterNotNull()
        val embeddedNavEnheter = Json.decodeFromString<List<EmbeddedNavEnhet?>>(string("nav_enheter")).filterNotNull()
        val kontaktpersoner = Json
            .decodeFromString<List<TiltaksgjennomforingKontaktperson?>>(string("kontaktpersoner"))
            .filterNotNull()

        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        return TiltaksgjennomforingAdminDto(
            id = uuid("id"),
            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakskode"),
            ),
            navn = string("navn"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            arrangor = TiltaksgjennomforingAdminDto.Arrangor(
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                navn = stringOrNull("arrangor_navn"),
                slettet = stringOrNull("arrangor_navn") == null,
                kontaktperson = uuidOrNull("arrangor_kontaktperson_id")?.let {
                    VirksomhetKontaktperson(
                        id = it,
                        organisasjonsnummer = string("arrangor_kontaktperson_organisasjonsnummer"),
                        navn = string("arrangor_kontaktperson_navn"),
                        telefon = stringOrNull("arrangor_kontaktperson_telefon"),
                        epost = string("arrangor_kontaktperson_epost"),
                        beskrivelse = stringOrNull("arrangor_kontaktperson_beskrivelse"),
                    )
                },
            ),
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet")?.let {
                Json.decodeFromString(
                    it,
                )
            },
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            apentForInnsok = boolean("apent_for_innsok"),
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            administratorer = administratorer,
            navEnheter = embeddedNavEnheter,
            navRegion = stringOrNull("nav_region_enhetsnummer")?.let {
                EmbeddedNavEnhet(
                    enhetsnummer = it,
                    navn = string("nav_region_navn"),
                    type = Norg2Type.valueOf(string("nav_region_type")),
                    overordnetEnhet = stringOrNull("nav_region_overordnet_enhet"),
                )
            },
            sanityId = uuidOrNull("sanity_id"),
            oppstart = TiltaksgjennomforingOppstartstype.valueOf(string("oppstart")),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            stengtFra = localDateOrNull("stengt_fra"),
            stengtTil = localDateOrNull("stengt_til"),
            kontaktpersoner = kontaktpersoner,
            stedForGjennomforing = stringOrNull("sted_for_gjennomforing"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            beskrivelse = stringOrNull("beskrivelse"),
            createdAt = localDateTime("created_at"),
            tilgjengeligForVeileder = boolean("tilgjengelig_for_veileder"),
            visesForVeileder = boolean("vises_for_veileder"),
            fremmoteTidspunkt = localDateTimeOrNull("fremmote_tidspunkt"),
            fremmoteSted = stringOrNull("fremmote_sted"),
        )
    }

    private fun Row.toTiltaksgjennomforingNotificationDto(): TiltaksgjennomforingNotificationDto {
        val administratorer = arrayOrNull<String?>("administratorer")?.asList()?.filterNotNull() ?: emptyList()

        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        return TiltaksgjennomforingNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            startDato = startDato,
            sluttDato = sluttDato,
            administratorer = administratorer,
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            stengtTil = localDateOrNull("stengt_til"),
        )
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
                   tg.tiltaksnummer,
                   tg.stengt_til
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

    fun setAvtaleId(tx: Session, gjennomforingId: UUID, avtaleId: UUID?) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return queryOf(query, avtaleId, gjennomforingId).asUpdate.let { tx.run(it) }
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(currentDate: LocalDate = LocalDate.now()): List<TiltaksgjennomforingNotificationDto> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.start_dato,
                   tg.slutt_dato,
                   tg.stengt_til,
                   array_agg(distinct a.nav_ident) as administratorer,
                   array_agg(e.enhetsnummer) as navEnheter,
                   tg.tiltaksnummer
            from tiltaksgjennomforing tg
                    left join tiltaksgjennomforing_administrator a on a.tiltaksgjennomforing_id = tg.id
                    left join tiltaksgjennomforing_nav_enhet e on e.tiltaksgjennomforing_id = tg.id
            where tg.stengt_til is not null and
               (?::timestamp + interval '7' day) = tg.stengt_til
               or (?::timestamp + interval '1' day) = tg.stengt_til
            group by tg.id;
        """.trimIndent()

        return queryOf(query, currentDate, currentDate).map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    fun setAvslutningsstatus(id: UUID, status: Avslutningsstatus): Int {
        return db.transaction { setAvslutningsstatus(it, id, status) }
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
}
