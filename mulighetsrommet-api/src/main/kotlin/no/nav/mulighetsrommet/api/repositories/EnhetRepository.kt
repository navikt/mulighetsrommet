package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.EnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.Norg2EnhetDbo
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class EnhetRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(enhet: Norg2EnhetDbo): QueryResult<Norg2EnhetDbo> = query {
        logger.info("Lagrer enhet id=${enhet.enhetId}")

        @Language("PostgreSQL")
        val query = """
            insert into enhet(enhetid, navn, enhetsnummer, status)
            values (:enhetId, :navn, :enhetsnummer, :status)
            on conflict (enhetId)
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
}

private fun Norg2EnhetDbo.toSqlParameters() = mapOf(
    "enhetId" to enhetId,
    "navn" to navn,
    "enhetsnummer" to enhetNr,
    "status" to status.name
)

private fun Row.toEnhetDbo() = Norg2EnhetDbo(
    enhetId = int("enhetId"),
    navn = string("navn"),
    enhetNr = string("enhetsNr"),
    status = EnhetStatus.valueOf(string("status"))
)
