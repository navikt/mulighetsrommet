package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
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
        logger.info("Lagrer enhet med enhetsnummer=${enhet.enhetNr}")

        @Language("PostgreSQL")
        val query = """
            insert into enhet(navn, enhetsnummer, status, type, overordnet_enhet)
            values (:navn, :enhetsnummer, :status, :type, :overordnet_enhet)
            on conflict (enhetsnummer)
                do update set   navn            = excluded.navn,
                                enhetsnummer    = excluded.enhetsnummer,
                                status          = excluded.status,
                                type            = excluded.type,
                                overordnet_enhet = excluded.overordnet_enhet
            returning *
        """.trimIndent()

        queryOf(query, enhet.toSqlParameters())
            .map { it.toEnhetDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(filter: EnhetFilter? = null): List<NavEnhetDbo> {
        val statuser = filter?.statuser ?: emptyList()
        logger.info("Henter enheter med status: ${statuser.joinToString(", ")}")
        val parameters = mapOf(
            "statuser" to db.createTextArray(statuser.map { it.name }),
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter?.statuser to "e.status = any(:statuser)",
        )

        @Language("PostgreSQL")
        val query = """
            select distinct e.navn,(e.enhetsnummer), e.status, e.type, e.overordnet_enhet
            from enhet e
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
            select distinct e.navn,(e.enhetsnummer), e.status, e.type, e.overordnet_enhet
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
            select navn, enhetsnummer, status, type, overordnet_enhet
            from enhet
            where enhetsnummer = ?
        """.trimIndent()

        return queryOf(query, enhet)
            .map { it.toEnhetDbo() }
            .asSingle
            .let { db.run(it) }
    }

    fun deleteWhereEnhetsnummer(enhetsnummerForSletting: List<String>) {
        logger.info("Sletter enheter med enhetsnummer: $enhetsnummerForSletting")

        val parameters = mapOf(
            "ider" to db.createTextArray(enhetsnummerForSletting)
        )

        @Language("PostgreSQL")
        val delete = """
            delete from enhet where enhetsnummer = any(:ider::text[])
        """.trimIndent()

        queryOf(delete, parameters)
            .asExecute
            .let { db.run(it) }
    }
}

private fun NavEnhetDbo.toSqlParameters() = mapOf(
    "navn" to navn,
    "enhetsnummer" to enhetNr,
    "status" to status.name,
    "type" to type.name,
    "overordnet_enhet" to overordnetEnhet
)

private fun Row.toEnhetDbo() = NavEnhetDbo(
    navn = string("navn"),
    enhetNr = string("enhetsnummer"),
    status = NavEnhetStatus.valueOf(string("status")),
    type = Norg2Type.valueOf(string("type")),
    overordnetEnhet = stringOrNull("overordnet_enhet")
)
