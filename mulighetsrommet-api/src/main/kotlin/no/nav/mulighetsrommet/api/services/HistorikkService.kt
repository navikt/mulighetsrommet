package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.arena.VeilarbarenaClient
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltaker
import org.intellij.lang.annotations.Language
import java.lang.Integer.parseInt

class HistorikkService(private val db: Database, private val veilarbarenaClient: VeilarbarenaClient) {
    suspend fun hentHistorikkForBruker(fnr: String, accessToken: String?): List<HistorikkForDeltaker> {
        val personId = veilarbarenaClient.hentPersonIdForFnr(fnr, accessToken)
        return getHistorikkForBrukerFromDb(parseInt(personId, 10))
    }

    private fun getHistorikkForBrukerFromDb(person_id: Int): List<HistorikkForDeltaker> {
        @Language("PostgreSQL")
        val query = """
            select deltaker.id, deltaker.fra_dato, deltaker.til_dato, status, tiltak.navn, tiltaksnummer, t.navn tiltakstype
         from deltaker
         left join tiltaksgjennomforing tiltak on tiltak.arena_id = deltaker.tiltaksgjennomforing_id
         left join tiltakstype t on tiltak.tiltakskode = t.tiltakskode
            where person_id = ?;
        """.trimIndent()
        val queryResult = queryOf(query, person_id).map { DatabaseMapper.toBrukerHistorikk(it) }.asList
        return db.run(queryResult)
    }
}
