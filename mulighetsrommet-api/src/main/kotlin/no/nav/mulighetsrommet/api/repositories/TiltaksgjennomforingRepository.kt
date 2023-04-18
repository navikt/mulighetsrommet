package no.nav.mulighetsrommet.api.repositories

import io.ktor.utils.io.core.*
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val TiltaksgjennomforingCutoffDate = LocalDate.of(2023, 1, 1)
    }

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<Unit> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, start_dato, slutt_dato, enhet, avslutningsstatus, tilgjengelighet, antall_plasser, avtale_id)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer, :start_dato, :slutt_dato, :enhet, :avslutningsstatus::avslutningsstatus, :tilgjengelighet::tilgjengelighetsstatus, :antall_plasser, :avtale_id)
            on conflict (id)
                do update set navn              = excluded.navn,
                              tiltakstype_id    = excluded.tiltakstype_id,
                              tiltaksnummer     = excluded.tiltaksnummer,
                              virksomhetsnummer = excluded.virksomhetsnummer,
                              start_dato        = excluded.start_dato,
                              slutt_dato        = excluded.slutt_dato,
                              enhet             = excluded.enhet,
                              avslutningsstatus = excluded.avslutningsstatus,
                              tilgjengelighet   = excluded.tilgjengelighet,
                              antall_plasser    = excluded.antall_plasser,
                              avtale_id         = excluded.avtale_id
            returning *
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

        db.transaction { tx ->
            tx.run(queryOf(query, tiltaksgjennomforing.toSqlParameters()).asExecute)

            tiltaksgjennomforing.ansvarlige.forEach { ansvarlig ->
                tx.run(
                    queryOf(
                        upsertAnsvarlig,
                        tiltaksgjennomforing.id,
                        ansvarlig
                    ).asExecute
                )
            }

            tx.run(
                queryOf(
                    deleteAnsvarlige,
                    tiltaksgjennomforing.id,
                    db.createTextArray(tiltaksgjennomforing.ansvarlige)
                ).asExecute
            )
        }
    }

    fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.tiltakstype_id,
                   tg.tiltaksnummer,
                   tg.virksomhetsnummer,
                   tg.start_dato,
                   tg.slutt_dato,
                   t.tiltakskode,
                   t.navn as tiltakstype_navn,
                   tg.enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige
            from tiltaksgjennomforing tg
                     inner join tiltakstype t on t.id = tg.tiltakstype_id
                     left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
            where tg.id = ?::uuid
            group by tg.id, t.id
        """.trimIndent()

        queryOf(query, id)
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(
        pagination: PaginationParams = PaginationParams(),
        filter: AdminTiltaksgjennomforingFilter
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> = query {
        val parameters = mapOf(
            "search" to "%${filter.search}%",
            "enhet" to filter.enhet,
            "tiltakstypeId" to filter.tiltakstypeId,
            "statuser" to filter.statuser?.let { db.createTextArray(it.map { it.name }) },
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "cutoffdato" to TiltaksgjennomforingCutoffDate
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.search to "(lower(tg.navn) like lower(:search))",
            filter.enhet to "lower(tg.enhet) = lower(:enhet)",
            filter.tiltakstypeId to "tg.tiltakstype_id = :tiltakstypeId",
            filter.statuser to "tg.avslutningsstatus = any(:statuser::avslutningsstatus[])",
            TiltaksgjennomforingCutoffDate to "(tg.slutt_dato >= :cutoffdato or tg.slutt_dato is null)",
        )

        val order = when (filter.sortering) {
            "navn-ascending" -> "tg.navn asc"
            "navn-descending" -> "tg.navn desc"
            else -> "tg.navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.tiltakstype_id,
                   tg.tiltaksnummer,
                   tg.virksomhetsnummer,
                   tg.start_dato,
                   tg.slutt_dato,
                   t.tiltakskode,
                   t.navn           as tiltakstype_navn,
                   tg.enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                   inner join tiltakstype t on tg.tiltakstype_id = t.id
                   left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
            $where
            group by tg.id, t.id
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

    fun getAllByTiltakstypeId(
        id: UUID,
        pagination: PaginationParams = PaginationParams()
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> = query {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.tiltakstype_id,
                   tg.tiltaksnummer,
                   tg.virksomhetsnummer,
                   tg.start_dato,
                   tg.slutt_dato,
                   t.tiltakskode,
                   t.navn           as tiltakstype_navn,
                   tg.enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                   inner join tiltakstype t on tg.tiltakstype_id = t.id
                   left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
            where tg.tiltakstype_id = ? and (tg.slutt_dato >= ? or tg.slutt_dato is null)
            group by tg.id, t.id
            order by tg.navn asc
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, id, TiltaksgjennomforingCutoffDate, pagination.limit, pagination.offset)
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
        pagination: PaginationParams
    ): QueryResult<List<TiltaksgjennomforingDbo>> = query {
        logger.info("Henter alle tiltaksgjennomføringer med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd, med avslutningsstatus $avslutningsstatus")

        @Language("PostgreSQL")
        val query = """
            select id::uuid,
                   navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   start_dato,
                   slutt_dato,
                   enhet,
                   avslutningsstatus,
                   tilgjengelighet,
                   antall_plasser,
                   avtale_id
            from tiltaksgjennomforing
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
                "cutoffdato" to TiltaksgjennomforingCutoffDate
            )
        )
            .map { it.toTiltaksgjennomforingDbo() }
            .asList
            .let { db.run(it) }
    }

    fun sok(filter: Sokefilter): QueryResult<List<TiltaksgjennomforingAdminDto>> = query {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tg.tiltakstype_id,
                   tg.tiltaksnummer,
                   tg.virksomhetsnummer,
                   tg.start_dato,
                   tg.slutt_dato,
                   t.tiltakskode,
                   t.navn as tiltakstype_navn,
                   tg.enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige
            from tiltaksgjennomforing tg
                   inner join tiltakstype t on tg.tiltakstype_id = t.id
                   left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
            where tiltaksnummer like concat('%', ?, '%') and (tg.slutt_dato >= :cutoffdato or tg.slutt_dato is null)
            group by tg.id, t.id
            order by tg.navn asc
        """.trimIndent()
        queryOf(query, filter.tiltaksnummer)
            .map {
                it.toTiltaksgjennomforingAdminDto()
            }
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

    private fun TiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "enhet" to enhet,
        "avslutningsstatus" to avslutningsstatus.name,
        "tilgjengelighet" to tilgjengelighet.name,
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId
    )

    private fun Row.toTiltaksgjennomforingDbo() = TiltaksgjennomforingDbo(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = string("virksomhetsnummer"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        enhet = string("enhet"),
        avslutningsstatus = Avslutningsstatus.valueOf(string("avslutningsstatus")),
        tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
        antallPlasser = intOrNull("antall_plasser"),
        avtaleId = uuidOrNull("avtale_id"),
        ansvarlige = emptyList(),
    )

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val sqlArray = sqlArrayOrNull("ansvarlige")?.let {
            (it.array as Array<String?>).asList().filterNotNull()
        } ?: emptyList()

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
            tiltaksnummer = string("tiltaksnummer"),
            virksomhetsnummer = string("virksomhetsnummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = string("enhet"),
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            ansvarlige = sqlArray,
        )
    }

    fun countGjennomforingerForTiltakstypeWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
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
}
