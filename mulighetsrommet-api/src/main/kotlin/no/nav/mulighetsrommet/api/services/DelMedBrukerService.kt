package no.nav.mulighetsrommet.api.services

import io.ktor.server.plugins.*
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.securelog.SecureLog
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class DelMedBrukerService(private val db: Database) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagreDelMedBruker(data: DelMedBrukerDbo): QueryResult<DelMedBrukerDbo> = query {
        SecureLog.logger.info("Veileder (${data.navident}) deler tiltak med tiltaksnummer: '${data.sanityId}' med bruker (${data.norskIdent})")

        if (data.norskIdent.trim().length != 11) {
            SecureLog.logger.warn("Brukers fnr er ikke 11 tegn. Innsendt: ${data.norskIdent}")
            throw BadRequestException("Brukers fnr er ikke 11 tegn")
        }

        if (data.navident.trim().isEmpty()) {
            SecureLog.logger.warn(
                "Veileders NAVident er tomt. Kan ikke lagre info om tiltak.",
            )
            throw BadRequestException("Veileders NAVident er ikke 6 tegn")
        }

        if (data.sanityId.trim().isEmpty()) {
            log.warn("Tiltaksnummer er ikke sendt med ved lagring av del med bruker. Kan derfor ikke lagre.")
            throw BadRequestException("Tiltaksnummer må inkluderes")
        }

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(norsk_ident, navident, sanity_id, dialogid, created_by, updated_by)
            values (:norsk_ident, :navident, :sanity_id, :dialogid, :created_by, :updated_by)
            returning *
        """.trimIndent()

        queryOf(query, data.toParameters())
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getDeltMedBruker(fnr: String, sanityId: String): QueryResult<DelMedBrukerDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select * from del_med_bruker where norsk_ident = ? and sanity_id = ? order by created_at desc limit 1
        """.trimIndent()
        queryOf(query, fnr, sanityId)
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it) }
    }

    private fun DelMedBrukerDbo.toParameters() = mapOf(
        "norsk_ident" to norskIdent,
        "navident" to navident,
        "sanity_id" to sanityId,
        "dialogid" to dialogId,
        "created_by" to navident,
        "updated_by" to navident,
    )

    private fun Row.toDelMedBruker(): DelMedBrukerDbo = DelMedBrukerDbo(
        id = string("id"),
        norskIdent = string("norsk_ident"),
        navident = string("navident"),
        sanityId = string("sanity_id"),
        dialogId = string("dialogid"),
        createdAt = localDateTime("created_at"),
        updatedAt = localDateTime("updated_at"),
        createdBy = string("created_by"),
        updatedBy = string("updated_by"),
    )
}
