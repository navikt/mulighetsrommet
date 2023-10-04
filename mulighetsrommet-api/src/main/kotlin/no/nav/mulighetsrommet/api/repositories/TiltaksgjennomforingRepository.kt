package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
                tilgjengelighet,
                estimert_ventetid,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav,
                stengt_fra,
                stengt_til,
                sted_for_gjennomforing,
                faneinnhold,
                beskrivelse
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
                :tilgjengelighet::tilgjengelighetsstatus,
                :estimert_ventetid,
                :antall_plasser,
                :avtale_id,
                :oppstart::tiltaksgjennomforing_oppstartstype,
                :opphav::opphav,
                :stengt_fra,
                :stengt_til,
                :sted_for_gjennomforing,
                :faneinnhold::jsonb,
                :beskrivelse
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
                              tilgjengelighet              = excluded.tilgjengelighet,
                              estimert_ventetid            = excluded.estimert_ventetid,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = excluded.oppstart,
                              opphav                       = excluded.opphav,
                              stengt_fra                   = excluded.stengt_fra,
                              stengt_til                   = excluded.stengt_til,
                              sted_for_gjennomforing       = excluded.sted_for_gjennomforing,
                              faneinnhold                  = excluded.faneinnhold,
                              beskrivelse                  = excluded.beskrivelse
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
                tilgjengelighet,
                antall_plasser,
                avtale_id,
                oppstart,
                opphav
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
                :tilgjengelighet::tilgjengelighetsstatus,
                :antall_plasser,
                :avtale_id,
                :oppstart::tiltaksgjennomforing_oppstartstype,
                :opphav::opphav
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
                              tilgjengelighet              = excluded.tilgjengelighet,
                              antall_plasser               = excluded.antall_plasser,
                              avtale_id                    = excluded.avtale_id,
                              oppstart                     = excluded.oppstart,
                              opphav                       = excluded.opphav
            returning *
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute.let { tx.run(it) }
    }

    fun updateEnheter(tiltaksnummer: String, navEnheter: List<String>): Int {
        @Language("PostgreSQL")
        val findId = """
            select id from tiltaksgjennomforing
                where (:aar::text is null and split_part(tiltaksnummer, '#', 2) = :lopenr)
                or (
                    :aar::text is not null and split_part(tiltaksnummer, '#', 1) = :aar
                    and split_part(tiltaksnummer, '#', 2) = :lopenr
                )
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
            insert into tiltaksgjennomforing_nav_enhet (tiltaksgjennomforing_id, enhetsnummer)
                values (:id::uuid, :enhetsnummer)
                on conflict (tiltaksgjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
            delete from tiltaksgjennomforing_nav_enhet
            where tiltaksgjennomforing_id = :id::uuid and not (enhetsnummer = any (:enhetsnummere))
        """.trimIndent()

        val (aar, lopenr) = tiltaksnummer.split("#").let {
            if (it.size == 2) {
                (it.first() to it[1])
            } else {
                (null to it.first())
            }
        }

        return db.transaction { tx ->
            val ider = queryOf(findId, mapOf("aar" to aar, "lopenr" to lopenr))
                .map { it.string("id") }
                .asList
                .let { db.run(it) }
            if (ider.size > 1) {
                throw PSQLException("Fant flere enn én tiltaksgjennomforing_id for tiltaksnummer: $tiltaksnummer", null)
            }
            if (ider.isEmpty()) {
                return@transaction 0
            }
            val id = ider[0]

            navEnheter.forEach { enhetsnummer ->
                tx.run(
                    queryOf(upsertEnhet, mapOf("id" to id, "enhetsnummer" to enhetsnummer)).asExecute,
                )
            }
            tx.run(
                queryOf(deleteEnheter, mapOf("id" to id, "enhetsnummere" to db.createTextArray(navEnheter))).asExecute,
            )

            1
        }
    }

    fun getBySanityId(sanityId: UUID): TiltaksgjennomforingAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltaksgjennomforing_admin_dto_view
            where sanity_id = ?
        """.trimIndent()

        return queryOf(query, sanityId)
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getBySanityIds(sanityIds: List<UUID>): Map<UUID, TiltaksgjennomforingAdminDto> {
        @Language("PostgreSQL")
        val query = """
            select * from tiltaksgjennomforing_admin_dto_view
            where sanity_id = any (?)
        """.trimIndent()

        return queryOf(query, db.createUuidArray(sanityIds))
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asList
            .let { db.run(it) }
            .filter { it.sanityId != null }
            .associateBy { it.sanityId!! }
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
        navEnhet: String? = null,
        tiltakstypeId: UUID? = null,
        status: Tiltaksgjennomforingsstatus? = null,
        sortering: String? = null,
        sluttDatoCutoff: LocalDate? = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
        dagensDato: LocalDate = LocalDate.now(),
        navRegion: String? = null,
        avtaleId: UUID? = null,
        arrangorOrgnr: String? = null,
        administratorNavIdent: String? = null,
        skalMigreres: Boolean? = null,
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        val parameters = mapOf(
            "search" to "%${search?.replace("/", "#")?.trim()}%",
            "navEnhet" to navEnhet,
            "tiltakstypeId" to tiltakstypeId,
            "status" to status,
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "cutoffdato" to sluttDatoCutoff,
            "today" to dagensDato,
            "navRegion" to navRegion,
            "avtaleId" to avtaleId,
            "arrangor_organisasjonsnummer" to arrangorOrgnr,
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "$it" }]""" },
            "skalMigreres" to skalMigreres,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            search to "((lower(navn) like lower(:search)) or (tiltaksnummer like :search))",
            navEnhet to "(:navEnhet in (select enhetsnummer from tiltaksgjennomforing_nav_enhet tg_e where tg_e.tiltaksgjennomforing_id = id) or arena_ansvarlig_enhet = :navEnhet)",
            tiltakstypeId to "tiltakstype_id = :tiltakstypeId",
            status to status?.toDbStatement(),
            sluttDatoCutoff to "(slutt_dato >= :cutoffdato or slutt_dato is null)",
            navRegion to "(arena_ansvarlig_enhet in (select enhetsnummer from nav_enhet where overordnet_enhet = :navRegion) or :navRegion in (select overordnet_enhet from nav_enhet inner join tiltaksgjennomforing_nav_enhet tg_e using(enhetsnummer) where tg_e.tiltaksgjennomforing_id = id) or :navRegion = arena_ansvarlig_enhet or :navRegion = navRegionEnhetsnummerForAvtale)",
            avtaleId to "avtale_id = :avtaleId",
            arrangorOrgnr to "arrangor_organisasjonsnummer = :arrangor_organisasjonsnummer",
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

    fun getTilgjengelighetsstatus(tiltaksnummer: String): TiltaksgjennomforingTilgjengelighetsstatus? {
        @Language("PostgreSQL")
        val query = """
            select tilgjengelighet
            from tiltaksgjennomforing
            where (:aar::text is null and split_part(tiltaksnummer, '#', 2) = :lopenr)
               or (:aar::text is not null and split_part(tiltaksnummer, '#', 1) = :aar and split_part(tiltaksnummer, '#', 2) = :lopenr)
        """.trimIndent()

        val parameters = tiltaksnummer.split("#").let {
            if (it.size == 2) {
                mapOf("aar" to it.first(), "lopenr" to it[1])
            } else {
                mapOf("aar" to null, "lopenr" to it.first())
            }
        }

        return queryOf(query, parameters)
            .map {
                val value = it.string("tilgjengelighet")
                TiltaksgjennomforingTilgjengelighetsstatus.valueOf(value)
            }
            .asSingle
            .let { db.run(it) }
    }

    private fun Tiltaksgjennomforingsstatus.toDbStatement(): String {
        return when (this) {
            Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK -> "(:today < start_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            Tiltaksgjennomforingsstatus.GJENNOMFORES -> "((:today >= start_dato and (:today <= slutt_dato or slutt_dato is null)) and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            Tiltaksgjennomforingsstatus.AVSLUTTET -> "(:today > slutt_dato or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            Tiltaksgjennomforingsstatus.AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            Tiltaksgjennomforingsstatus.AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVLYST}'"
        }
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
        "tilgjengelighet" to tilgjengelighet.name,
        "estimert_ventetid" to estimertVentetid,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "opphav" to opphav.name,
        "stengt_fra" to stengtFra,
        "stengt_til" to stengtTil,
        "sted_for_gjennomforing" to stedForGjennomforing,
        "faneinnhold" to Json.encodeToString(faneinnhold),
        "beskrivelse" to beskrivelse,
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
        "tilgjengelighet" to tilgjengelighet.name,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
        "oppstart" to oppstart.name,
        "opphav" to opphav.name,
    )

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val administratorer = Json
            .decodeFromString<List<TiltaksgjennomforingAdminDto.Administrator?>>(string("administratorer"))
            .filterNotNull()
        val navEnheter = Json.decodeFromString<List<NavEnhet?>>(string("nav_enheter")).filterNotNull()
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
            arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet"),
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
            estimertVentetid = stringOrNull("estimert_ventetid"),
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            administrator = administratorer.getOrNull(0),
            navEnheter = navEnheter,
            navRegion = stringOrNull("navRegionEnhetsnummerForAvtale")?.let {
                NavEnhet(
                    enhetsnummer = it,
                    navn = string("navRegionForAvtale"),
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
            updatedAt = localDateTime("updated_at"),
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

    fun countGjennomforingerForTiltakstypeWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        @Language("PostgreSQL")
        val query = """
            SELECT count(id) AS antall
            FROM tiltaksgjennomforing
            WHERE tiltakstype_id = ?
            and start_dato < ?::timestamp
            and slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, id, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
    }

    fun countDeltakereForAvtaleWithId(avtaleId: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        @Language("PostgreSQL")
        val query = """
            SELECT count(*) AS antall
            FROM tiltaksgjennomforing tg
            join deltaker d on d.tiltaksgjennomforing_id = tg.id
            where tg.avtale_id = ?::uuid
            and d.start_dato < ?::timestamp
            and d.slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, avtaleId, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
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
            group by tg.id, a.nav_ident;
        """.trimIndent()

        return queryOf(query, currentDate, currentDate, currentDate).map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    fun updateAvtaleIdForGjennomforing(gjennomforingId: UUID, avtaleId: UUID?) {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avtale_id = ?
            where id = ?
        """.trimIndent()

        return queryOf(query, avtaleId, gjennomforingId).asUpdate.let { db.run(it) }
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
            group by tg.id, a.nav_ident;
        """.trimIndent()

        return queryOf(query, currentDate, currentDate).map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    fun avbrytGjennomforing(gjennomforingId: UUID, tx: Session): Int {
        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing
            set avslutningsstatus = 'AVBRUTT'
            where id = ?::uuid
        """.trimIndent()

        return tx.run(queryOf(query, gjennomforingId).asUpdate)
    }
}
