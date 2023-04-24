package no.nav.mulighetsrommet.api.repositories

import io.ktor.utils.io.core.*
import kotliquery.Row
import kotliquery.queryOf
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
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, arena_ansvarlig_enhet, start_dato, slutt_dato, avslutningsstatus, tilgjengelighet, antall_plasser, avtale_id)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer, :arena_ansvarlig_enhet, :start_dato, :slutt_dato, :avslutningsstatus::avslutningsstatus, :tilgjengelighet::tilgjengelighetsstatus, :antall_plasser, :avtale_id)
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
                              antall_plasser        = excluded.antall_plasser,
                              avtale_id             = excluded.avtale_id
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into tiltaksgjennomforing_enhet (tiltaksgjennomforing_id, enhetsnummer)
             values (?::uuid, ?)
             on conflict (tiltaksgjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from tiltaksgjennomforing_enhet
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

            tiltaksgjennomforing.enheter.forEach { enhetId ->
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
                    db.createTextArray(tiltaksgjennomforing.enheter),
                ).asExecute,
            )
        }
    }

    fun updateEnheter(tiltaksnummer: String, enheter: List<String>): QueryResult<Int> = query {
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
            insert into tiltaksgjennomforing_enhet (tiltaksgjennomforing_id, enhetsnummer)
                values (:id::uuid, :enhetsnummer)
                on conflict (tiltaksgjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
            delete from tiltaksgjennomforing_enhet
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
                throw PSQLException("Fant flere enn én tiltaksgjennomforing_id for tiltaksnummer: ${tiltaksnummer}", null)
            }
            if (ider.isEmpty()) {
                return@transaction 0
            }
            val id = ider[0]

            enheter.forEach { enhetsnummer ->
                tx.run(
                    queryOf( upsertEnhet, mapOf("id" to id, "enhetsnummer" to enhetsnummer)).asExecute,
                )
            }
            tx.run(
                queryOf(deleteEnheter, mapOf("id" to id, "enhetsnummere" to db.createTextArray(enheter))).asExecute
            )

            1
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
                   tg.arena_ansvarlig_enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige,
                   array_agg(e.enhetsnummer) as enheter
            from tiltaksgjennomforing tg
                     inner join tiltakstype t on t.id = tg.tiltakstype_id
                     left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
                     left join tiltaksgjennomforing_enhet e on e.tiltaksgjennomforing_id = tg.id
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
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.search to "(lower(tg.navn) like lower(:search))",
            filter.enhet to "lower(tg.arena_ansvarlig_enhet) = lower(:enhet)",
            filter.tiltakstypeId to "tg.tiltakstype_id = :tiltakstypeId",
            filter.status to filter.status?.toDbStatement(),
            filter.sluttDatoCutoff to "(tg.slutt_dato >= :cutoffdato or tg.slutt_dato is null)",
            filter.fylkesenhet to "tg.arena_ansvarlig_enhet in (select enhetsnummer from enhet where overordnet_enhet = :fylkesenhet)",
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
                   tg.arena_ansvarlig_enhet,
                   tg.avslutningsstatus,
                   tg.tilgjengelighet,
                   tg.antall_plasser,
                   tg.avtale_id,
                   array_agg(a.navident) as ansvarlige,
                   array_agg(e.enhetsnummer) as enheter,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                   inner join tiltakstype t on tg.tiltakstype_id = t.id
                   left join tiltaksgjennomforing_ansvarlig a on a.tiltaksgjennomforing_id = tg.id
                   left join tiltaksgjennomforing_enhet e on e.tiltaksgjennomforing_id = tg.id
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

    fun getAllByDateIntervalAndAvslutningsstatus(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        avslutningsstatus: Avslutningsstatus,
        pagination: PaginationParams,
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
                   arena_ansvarlig_enhet,
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
        "antall_plasser" to antallPlasser,
        "avtale_id" to avtaleId,
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
        antallPlasser = intOrNull("antall_plasser"),
        avtaleId = uuidOrNull("avtale_id"),
        ansvarlige = emptyList(),
        enheter = emptyList(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val ansvarlige = sqlArrayOrNull("ansvarlige")?.let {
            (it.array as Array<String?>).asList().filterNotNull()
        } ?: emptyList()
        val enheter = sqlArrayOrNull("enheter")?.let {
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
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            virksomhetsnummer = string("virksomhetsnummer"),
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
            antallPlasser = intOrNull("antall_plasser"),
            avtaleId = uuidOrNull("avtale_id"),
            ansvarlige = ansvarlige,
            enheter = enheter,
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
