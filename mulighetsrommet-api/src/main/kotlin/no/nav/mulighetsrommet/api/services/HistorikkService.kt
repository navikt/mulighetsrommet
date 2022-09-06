package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import java.lang.Integer.parseInt

class HistorikkService(private val db: Database) {
    suspend fun hentHistorikkForBruker(fnr: String): List<HistorikkForBruker> {
        return getHistorikkForBrukerFromDb(parseInt(fnr, 10))
    }

    private fun getHistorikkForBrukerFromDb(fnr: Int): List<HistorikkForBruker> {
        @Language("PostgreSQL")
        val query = """
            select deltaker.id deltaker_id, deltaker.fra_dato, deltaker.til_dato, status, tiltak.navn, tiltaksnummer, t.navn tiltakstype
            from deltaker
                 left join tiltaksgjennomforing tiltak on tiltak.id = deltaker.tiltaksgjennomforing_id
                 left join tiltakstype t on tiltak.tiltakskode = t.tiltakskode
            where person_id = ?;
        """.trimIndent()
        val queryResult = queryOf(query, fnr).map { DatabaseMapper.toBrukerHistorikk(it) }.asList
        return db.run(queryResult)
    }
}

@Serializable
data class HistorikkForBruker(
    val id: String
)
