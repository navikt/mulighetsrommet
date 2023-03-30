package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class AvtaleRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtale: AvtaleDbo): QueryResult<AvtaleDbo> = query {
        logger.info("Lagrer avtale id=${avtale.id}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               leverandor_organisasjonsnummer,
                               start_dato,
                               slutt_dato,
                               enhet,
                               avtaletype,
                               avslutningsstatus,
                               prisbetingelser,
                               antall_plasser,
                               url,
                               opphav)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :leverandor_organisasjonsnummer,
                    :start_dato,
                    :slutt_dato,
                    :enhet,
                    :avtaletype::avtaletype,
                    :avslutningsstatus::avslutningsstatus,
                    :prisbetingelser,
                    :antall_plasser,
                    :url,
                    :opphav::avtaleopphav)
            on conflict (id) do update set navn                           = excluded.navn,
                                           tiltakstype_id                 = excluded.tiltakstype_id,
                                           avtalenummer                   = excluded.avtalenummer,
                                           leverandor_organisasjonsnummer = excluded.leverandor_organisasjonsnummer,
                                           start_dato                     = excluded.start_dato,
                                           slutt_dato                     = excluded.slutt_dato,
                                           enhet                          = excluded.enhet,
                                           avtaletype                     = excluded.avtaletype,
                                           avslutningsstatus              = excluded.avslutningsstatus,
                                           prisbetingelser                = excluded.prisbetingelser,
                                           antall_plasser                 = excluded.antall_plasser,
                                           url                            = excluded.url,
                                           opphav                         = excluded.opphav
            returning *
        """.trimIndent()

        // TODO: Endre her hvis man skal kunne oppdatere den ansvarlige
        val queryForAnsvarlig = """
             insert into avtale_ansvarlig (avtale_id, navident)
             values (?::uuid, ?)
        """.trimIndent()

        db.transaction { transactionalSession ->
            val result = queryOf(query, avtale.toSqlParameters())
                .map { it.toAvtaleDbo() }
                .asSingle
                .let { transactionalSession.run(it)!! }

            queryOf(queryForAnsvarlig, avtale.id, avtale.ansvarlig).asExecute.let { transactionalSession.run(it) }

            result
        }
    }

    fun get(id: UUID): AvtaleAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.enhet,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode
            from avtale a
                     join tiltakstype t on t.id = a.tiltakstype_id
            where a.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toAvtaleAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter avtale id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from avtale
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams()
    ): Pair<Int, List<AvtaleAdminDto>> {
        if (filter.tiltakstypeId != null) {
            logger.info("Henter avtaler for tiltakstype med id: '${filter.tiltakstypeId}'")
        } else {
            logger.info("Henter alle avtaler")
        }
        val parameters = mapOf(
            "tiltakstype_id" to filter.tiltakstypeId,
            "search" to "%${filter.search}%",
            "enhet" to filter.enhet,
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "today" to filter.dagensDato
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.tiltakstypeId to "a.tiltakstype_id = :tiltakstype_id",
            filter.search to "(lower(a.navn) like lower(:search))",
            filter.avtalestatus to filter.avtalestatus?.toDbStatement(),
            filter.enhet to "lower(a.enhet) = lower(:enhet)"
        )

        val order = when (filter.sortering) {
            "navn-ascending" -> "a.navn asc"
            "navn-descending" -> "a.navn desc"
            "status-ascending" -> "a.avslutningsstatus asc, a.start_dato asc, a.slutt_dato desc"
            "status-descending" -> "a.avslutningsstatus desc, a.slutt_dato asc, a.start_dato desc"
            else -> "a.navn asc"
        }

        @Language("PostgreSQL")
        val query = """
           select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.enhet,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode,
                   count(*) over () as full_count
            from avtale a
                     join tiltakstype t on a.tiltakstype_id = t.id
            $where
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toAvtaleAdminDto()
            }
            .asList
            .let { db.run(it) }

        val totaltAntall = results.firstOrNull()?.first ?: 0
        val avtaler = results.map { it.second }
        return Pair(totaltAntall, avtaler)
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "leverandor_organisasjonsnummer" to leverandorOrganisasjonsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "enhet" to enhet,
        "avtaletype" to avtaletype.name,
        "avslutningsstatus" to avslutningsstatus.name,
        "prisbetingelser" to prisbetingelser,
        "antall_plasser" to antallPlasser,
        "url" to url,
        "opphav" to opphav.name
    )

    private fun Row.toAvtaleDbo(): AvtaleDbo {
        return AvtaleDbo(
            id = uuid("id"),
            navn = string("navn"),
            tiltakstypeId = uuid("tiltakstype_id"),
            avtalenummer = stringOrNull("avtalenummer"),
            leverandorOrganisasjonsnummer = string("leverandor_organisasjonsnummer"),
            startDato = localDate("start_dato"),
            sluttDato = localDate("slutt_dato"),
            enhet = string("enhet"),
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            avslutningsstatus = Avslutningsstatus.valueOf(string("avslutningsstatus")),
            prisbetingelser = stringOrNull("prisbetingelser"),
            antallPlasser = intOrNull("antall_plasser"),
            url = stringOrNull("url"),
            opphav = AvtaleDbo.Opphav.valueOf(string("opphav"))
        )
    }

    private fun Row.toAvtaleAdminDto(): AvtaleAdminDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDate("slutt_dato")
        return AvtaleAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            tiltakstype = AvtaleAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakskode")
            ),
            avtalenummer = stringOrNull("avtalenummer"),
            leverandor = AvtaleAdminDto.Leverandor(
                organisasjonsnummer = string("leverandor_organisasjonsnummer")
            ),
            startDato = startDato,
            sluttDato = sluttDato,
            navEnhet = AvtaleAdminDto.NavEnhet(
                enhetsnummer = string("enhet"),
            ),
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            avtalestatus = Avtalestatus.resolveFromDatesAndAvslutningsstatus(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus"))
            ),
            prisbetingelser = stringOrNull("prisbetingelser")
        )
    }

    private fun Avtalestatus.toDbStatement(): String {
        return when (this) {
            Avtalestatus.Aktiv -> "((:today >= start_dato and :today <= slutt_dato) and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            Avtalestatus.Avsluttet -> "(:today > slutt_dato or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            Avtalestatus.Avbrutt -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            Avtalestatus.Planlagt -> "(:today < start_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
        }
    }

    fun countAktiveAvtalerForTiltakstypeWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        val query = """
             SELECT count(id) AS antall
             FROM avtale
             WHERE tiltakstype_id = ?
             and start_dato < ?::timestamp
             and slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, id, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
    }

    fun countTiltaksgjennomforingerForAvtaleWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        val query = """
            select count(*) as antall
            from tiltaksgjennomforing
            where avtale_id::uuid = ?
            and start_dato < ?::timestamp
            and slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, id, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
    }
}
