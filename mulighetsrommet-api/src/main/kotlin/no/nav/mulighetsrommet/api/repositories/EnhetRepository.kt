package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class EnhetRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(enhet: NavEnhetDbo): QueryResult<NavEnhetDbo> = query {
        logger.info("Lagrer enhet id=${enhet.enhetId}")

        @Language("PostgreSQL")
        val query = """
            insert into enhet(enhet_id, navn, enhetsnummer, status)
            values (:enhet_id, :navn, :enhetsnummer, :status)
            on conflict (enhet_id)
                do update set   navn            = excluded.navn,
                                enhetsnummer    = excluded.enhetsnummer,
                                status          = excluded.status
            returning *
        """.trimIndent()

        queryOf(query, enhet.toSqlParameters())
            .map { it.toEnhetDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(filter: EnhetFilter): List<NavEnhetDbo> {
        val statuser = filter.statuser ?: emptyList()
        logger.info("Henter enheter med status: ${statuser.joinToString(", ")}")
        val parameters = mapOf(
            "statuser" to db.createTextArray(statuser.map { it.name }),
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.statuser to "e.status = any(:statuser)",
        )

        val join = if (filter.enhetMaaHaTiltaksgjennomforing == true) {
            """
                join tiltaksgjennomforing tg on e.enhetsnummer = tg.enhet
            """.trimIndent()
        } else {
            ""
        }

        @Language("PostgreSQL")
        val query = """
            select distinct e.navn,(e.enhetsnummer), e.enhet_id, e.status
            from enhet e
            $join
            $where
            order by e.navn asc
        """.trimIndent()

        return queryOf(query, parameters)
            .map { it.toEnhetDbo() }
            .asList
            .let { db.run(it) }
    }

    fun getAllEnheterWithAvtale(filter: EnhetFilter): List<NavEnhetDbo> {
        val statuser = filter.statuser ?: emptyList()
        logger.info("Henter enheter med status: ${statuser.joinToString(", ")}")
        val parameters = mapOf(
            "statuser" to db.createTextArray(statuser.map { it.name }),
            "tiltakstypeId" to filter.tiltakstypeId
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.statuser to "e.status = any(:statuser)",
            filter.tiltakstypeId to "a.tiltakstype_id = :tiltakstypeId::uuid"
        )

        @Language("PostgreSQL")
        val query = """
            select distinct e.navn,(e.enhetsnummer), e.enhet_id, e.status
            from enhet e
            join avtale a
            on a.enhet = e.enhetsnummer
            $where
            order by e.navn asc
        """.trimIndent()

        return queryOf(query, parameters)
            .map { it.toEnhetDbo() }
            .asList
            .let { db.run(it) }
    }

    fun get(enhet: String): NavEnhetDbo? {
        @Language("PostgreSQL")
        val query = """
            select navn, enhet_id, enhetsnummer, status
            from enhet
            where enhetsnummer = ?
        """.trimIndent()

        return queryOf(query, enhet)
            .map { it.toEnhetDbo() }
            .asSingle
            .let { db.run(it) }
    }
}

private fun NavEnhetDbo.toSqlParameters() = mapOf(
    "enhet_id" to enhetId,
    "navn" to navn,
    "enhetsnummer" to enhetNr,
    "status" to status.name
)

private fun Row.toEnhetDbo() = NavEnhetDbo(
    enhetId = int("enhet_id"),
    navn = string("navn"),
    enhetNr = string("enhetsnummer"),
    status = NavEnhetStatus.valueOf(string("status"))
)
