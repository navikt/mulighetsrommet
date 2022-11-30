package no.nav.mulighetsrommet.api.services

import io.ktor.server.plugins.*
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.DelMedBruker
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class DelMedBrukerService(private val db: Database) {
    private val secureLog = SecureLog.logger
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun lagreDelMedBruker(data: DelMedBruker): QueryResult<DelMedBruker> = query {
        secureLog.info("Veileder (${data.navident}) deler tiltak med tiltaksnummer: '${data.tiltaksnummer}' med bruker (${data.bruker_fnr})")

        if (data.bruker_fnr.trim().length != 11) {
            secureLog.warn("Brukers fnr er ikke 11 tegn. Innsendt: ${data.bruker_fnr}")
            throw BadRequestException("Brukers fnr er ikke 11 tegn")
        }

        if (data.navident.trim().isEmpty()) {
            secureLog.warn(
                "Veileders NAVident er tomt. Kan ikke lagre info om tiltak."
            )
            throw BadRequestException("Veileders NAVident er ikke 6 tegn")
        }

        if (data.tiltaksnummer.trim().isEmpty()) {
            log.warn("Tiltaksnummer er ikke sendt med ved lagring av del med bruker. Kan derfor ikke lagre.")
            throw BadRequestException("Tiltaksnummer må inkluderes")
        }

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(bruker_fnr, navident, tiltaksnummer, dialogId, created_by, updated_by) 
            values(?, ?, ?, ?, ?, ?)
            returning *
        """.trimIndent()
        data.run {
            queryOf(
                query,
                data.bruker_fnr,
                data.navident,
                data.tiltaksnummer,
                data.dialogId,
                data.navident,
                data.navident
            )
        }
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getDeltMedBruker(fnr: String, tiltaksnummer: String): QueryResult<DelMedBruker?> = query {
        @Language("PostgreSQL")
        val query = """
            select * from del_med_bruker where bruker_fnr = ? and tiltaksnummer = ? order by created_at desc
        """.trimIndent()
        queryOf(
            query,
            fnr,
            tiltaksnummer
        ).map { it.toDelMedBruker() }.asSingle.let { db.run(it) }
    }

    private fun Row.toDelMedBruker(): DelMedBruker = DelMedBruker(
        id = string("id"),
        bruker_fnr = string("bruker_fnr"),
        navident = string("navident"),
        tiltaksnummer = string("tiltaksnummer"),
        dialogId = string("dialogId"),
        created_at = localDateTime("created_at"),
        updated_at = localDateTime("updated_at"),
        created_by = string("created_by"),
        updated_by = string("updated_by")
    )
}
