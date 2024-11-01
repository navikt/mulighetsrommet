package no.nav.mulighetsrommet.api.navenhet.db

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class NavEnhetRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(enhet: NavEnhetDbo): QueryResult<NavEnhetDbo> = query {
        logger.info("Lagrer enhet med enhetsnummer=${enhet.enhetsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into nav_enhet(navn, enhetsnummer, status, type, overordnet_enhet)
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

    fun getAll(
        statuser: List<NavEnhetStatus>? = null,
        typer: List<Norg2Type>? = null,
        overordnetEnhet: String? = null,
    ): List<NavEnhetDbo> {
        logger.info("Henter enheter med status: ${statuser?.joinToString(", ")}")

        val parameters = mapOf(
            "statuser" to statuser?.let { items -> db.createTextArray(items.map { it.name }) },
            "typer" to typer?.let { items -> db.createTextArray(items.map { it.name }) },
            "overordnet_enhet" to overordnetEnhet,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            statuser to "e.status = any(:statuser)",
            typer to "e.type = any(:typer)",
            overordnetEnhet to "e.overordnet_enhet = :overordnet_enhet",
        )

        @Language("PostgreSQL")
        val query = """
            select distinct e.navn, e.enhetsnummer, e.status, e.type, e.overordnet_enhet
            from nav_enhet e
            $where
            order by e.navn
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
            from nav_enhet
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
            "ider" to db.createTextArray(enhetsnummerForSletting),
        )

        @Language("PostgreSQL")
        val delete = """
            delete from nav_enhet where enhetsnummer = any(:ider::text[])
        """.trimIndent()

        queryOf(delete, parameters)
            .asExecute
            .let { db.run(it) }
    }
}

private fun NavEnhetDbo.toSqlParameters() = mapOf(
    "navn" to navn,
    "enhetsnummer" to enhetsnummer,
    "status" to status.name,
    "type" to type.name,
    "overordnet_enhet" to overordnetEnhet,
)

private fun Row.toEnhetDbo() = NavEnhetDbo(
    navn = string("navn"),
    enhetsnummer = string("enhetsnummer"),
    status = NavEnhetStatus.valueOf(string("status")),
    type = Norg2Type.valueOf(string("type")),
    overordnetEnhet = stringOrNull("overordnet_enhet"),
)
