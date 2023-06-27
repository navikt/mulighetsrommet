package no.nav.mulighetsrommet.api.repositories

import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<Unit> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")
        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (
                id,
                navn,
                tiltakstype_id,
                tiltaksnummer,
                virksomhetsnummer,
                arena_ansvarlig_enhet,
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
                lokasjon_arrangor
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :tiltaksnummer,
                :virksomhetsnummer,
                :arena_ansvarlig_enhet,
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
                :lokasjon_arrangor
            )
            on conflict (id)
                do update set navn                  = excluded.navn,
                              tiltakstype_id        = excluded.tiltakstype_id,
                              tiltaksnummer         = excluded.tiltaksnummer,
                              virksomhetsnummer     = excluded.virksomhetsnummer,
                              arena_ansvarlig_enhet = excluded.arena_ansvarlig_enhet,
                              start_dato            = excluded.start_dato,
                              slutt_dato            = excluded.slutt_dato,
                              avslutningsstatus     = excluded.avslutningsstatus,
                              tilgjengelighet       = excluded.tilgjengelighet,
                              estimert_ventetid     = excluded.estimert_ventetid,
                              antall_plasser        = excluded.antall_plasser,
                              avtale_id             = excluded.avtale_id,
                              oppstart              = excluded.oppstart,
                              opphav                = excluded.opphav,
                              stengt_fra            = excluded.stengt_fra,
                              stengt_til            = excluded.stengt_til,
                              lokasjon_arrangor              = excluded.lokasjon_arrangor
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
        val upsertAnsvarlig = """
             insert into tiltaksgjennomforing_ansvarlig (tiltaksgjennomforing_id, navident)
             values (?::uuid, ?)
             on conflict (tiltaksgjennomforing_id, navident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAnsvarlige = """
             delete from tiltaksgjennomforing_ansvarlig
             where tiltaksgjennomforing_id = ?::uuid and not (navident = any (?))
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

        db.transaction { tx ->
            tx.run(queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute)

            tiltaksgjennomforing.ansvarlige.forEach { ansvarlig ->
                tx.run(
                    queryOf(
                        upsertAnsvarlig,
                        tiltaksgjennomforing.id,
                        ansvarlig,
                    ).asExecute,
                )
            }

            tx.run(
                queryOf(
                    deleteAnsvarlige,
                    tiltaksgjennomforing.id,
                    db.createTextArray(tiltaksgjennomforing.ansvarlige),
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
    }

    fun updateEnheter(tiltaksnummer: String, navEnheter: List<String>): QueryResult<Int> = query {
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

        db.transaction { tx ->
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

    fun getBySanityIds(sanityIds: List<UUID>): Map<String, TiltaksgjennomforingAdminDto> {
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

    fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltaksgjennomforing_admin_dto_view
            where id = ?::uuid
        """.trimIndent()
        queryOf(query, id)
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun updateSanityTiltaksgjennomforingId(id: UUID, sanityId: UUID): QueryResult<Unit> = query {
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
            .let { db.run(it) }
    }

    fun getAll(
        pagination: PaginationParams = PaginationParams(),
        filter: AdminTiltaksgjennomforingFilter,
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> = query {
        val parameters = mapOf(
            "search" to "%${filter.search}%",
            "enhet" to filter.enhet,
            "tiltakstypeId" to filter.tiltakstypeId,
            "status" to filter.status,
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "cutoffdato" to filter.sluttDatoCutoff,
            "today" to filter.dagensDato,
            "fylkesenhet" to filter.fylkesenhet,
            "avtaleId" to filter.avtaleId,
            "virksomhetsnummer" to filter.arrangorOrgnr,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.search to "((lower(navn) like lower(:search)) or (tiltaksnummer like :search))",
            filter.enhet to "lower(arena_ansvarlig_enhet) = lower(:enhet)",
            filter.tiltakstypeId to "tiltakstype_id = :tiltakstypeId",
            filter.status to filter.status?.toDbStatement(),
            filter.sluttDatoCutoff to "(slutt_dato >= :cutoffdato or slutt_dato is null)",
            filter.fylkesenhet to "arena_ansvarlig_enhet in (select enhetsnummer from nav_enhet where overordnet_enhet = :fylkesenhet)",
            filter.avtaleId to "avtale_id = :avtaleId",
            filter.arrangorOrgnr to "virksomhetsnummer = :virksomhetsnummer",
        )

        val order = when (filter.sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "tiltaksnummer-ascending" -> "tiltaksnummer asc"
            "tiltaksnummer-descending" -> "tiltaksnummer desc"
            "arrangor-ascending" -> "virksomhetsnavn asc"
            "arrangor-descending" -> "virksomhetsnavn desc"
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

        Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun getAllByDateIntervalAndAvslutningsstatus(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        avslutningsstatus: Avslutningsstatus,
        pagination: PaginationParams,
    ): QueryResult<List<TiltaksgjennomforingDbo>> = query {
        logger.info("Henter alle tiltaksgjennomføringer med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd, med avslutningsstatus $avslutningsstatus")

        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.tiltakstype_id,
                   tg.tiltaksnummer,
                   tg.virksomhetsnummer,
                   tg.start_dato,
                   tg.slutt_dato,
                   tg.arena_ansvarlig_enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.estimert_ventetid,
                   tg.antall_plasser,
                   tg.avtale_id,
                   tg.oppstart,
                   tg.opphav
            from tiltaksgjennomforing tg
            where avslutningsstatus = :avslutningsstatus::avslutningsstatus and (
                (start_dato > :date_interval_start and start_dato <= :date_interval_end) or
                (slutt_dato >= :date_interval_start and slutt_dato < :date_interval_end))
            order by id
            limit :limit offset :offset
        """.trimIndent()

        queryOf(
            query,
            mapOf(
                "avslutningsstatus" to avslutningsstatus.name,
                "date_interval_start" to dateIntervalStart,
                "date_interval_end" to dateIntervalEnd,
                "limit" to pagination.limit,
                "offset" to pagination.offset,
            ),
        )
            .map { it.toTiltaksgjennomforingDbo() }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getTilgjengelighetsstatus(tiltaksnummer: String): TiltaksgjennomforingDbo.Tilgjengelighetsstatus? {
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
                TiltaksgjennomforingDbo.Tilgjengelighetsstatus.valueOf(value)
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
        "virksomhetsnummer" to virksomhetsnummer,
        "start_dato" to startDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
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
        "lokasjon_arrangor" to lokasjonArrangor,
    )

    private fun Row.toTiltaksgjennomforingDbo() = TiltaksgjennomforingDbo(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = stringOrNull("tiltaksnummer"),
        virksomhetsnummer = string("virksomhetsnummer"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet"),
        avslutningsstatus = Avslutningsstatus.valueOf(string("avslutningsstatus")),
        tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
        estimertVentetid = stringOrNull("estimert_ventetid"),
        antallPlasser = intOrNull("antall_plasser"),
        avtaleId = uuidOrNull("avtale_id"),
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingDbo.Oppstartstype.valueOf(string("oppstart")),
        opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
    )

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val ansvarlige = arrayOrNull<String?>("ansvarlige")?.asList()?.filterNotNull() ?: emptyList()
        val navEnheter = Json.decodeFromString<List<NavEnhet?>>(string("nav_enheter")).filterNotNull()
        val kontaktpersoner =
            Json.decodeFromString<List<TiltaksgjennomforingKontaktperson?>>(string("kontaktpersoner")).filterNotNull()

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
            virksomhetsnummer = string("virksomhetsnummer"),
            virksomhetsnavn = stringOrNull("virksomhetsnavn"),
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet"),
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
            estimertVentetid = stringOrNull("estimert_ventetid"),
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            ansvarlig = ansvarlige.getOrNull(0),
            navEnheter = navEnheter,
            navRegion = stringOrNull("navRegionForAvtale"),
            sanityId = stringOrNull("sanity_id"),
            oppstart = TiltaksgjennomforingDbo.Oppstartstype.valueOf(string("oppstart")),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            stengtFra = localDateOrNull("stengt_fra"),
            stengtTil = localDateOrNull("stengt_til"),
            kontaktpersoner = kontaktpersoner,
            lokasjonArrangor = stringOrNull("lokasjon_arrangor"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Row.toTiltaksgjennomforingNotificationDto(): TiltaksgjennomforingNotificationDto {
        val ansvarlige = arrayOrNull<String?>("ansvarlige")?.asList()?.filterNotNull() ?: emptyList()

        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        return TiltaksgjennomforingNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            startDato = startDato,
            sluttDato = sluttDato,
            ansvarlige = ansvarlige,
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
                   array_agg(distinct a.navident) as ansvarlige,
                   array_agg(e.enhetsnummer) as navEnheter,
                   tg.tiltaksnummer,
                   tg.stengt_til
            from tiltaksgjennomforing tg
                     left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
                    left join tiltaksgjennomforing_nav_enhet e on e.tiltaksgjennomforing_id = tg.id
            where (?::timestamp + interval '14' day) = tg.slutt_dato
               or (?::timestamp + interval '7' day) = tg.slutt_dato
               or (?::timestamp + interval '1' day) = tg.slutt_dato
            group by tg.id, a.navident;
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
                   array_agg(distinct a.navident) as ansvarlige,
                   array_agg(e.enhetsnummer) as navEnheter,
                   tg.tiltaksnummer
            from tiltaksgjennomforing tg
                    left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
                    left join tiltaksgjennomforing_nav_enhet e on e.tiltaksgjennomforing_id = tg.id
            where tg.stengt_til is not null and
               (?::timestamp + interval '7' day) = tg.stengt_til
               or (?::timestamp + interval '1' day) = tg.stengt_til
            group by tg.id, a.navident;
        """.trimIndent()

        return queryOf(query, currentDate, currentDate).map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .let { db.run(it) }
    }
}
