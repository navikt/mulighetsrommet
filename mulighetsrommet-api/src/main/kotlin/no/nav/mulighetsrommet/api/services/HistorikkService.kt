package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.arena.VeilarbarenaClient
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltaker
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Integer.parseInt

class HistorikkService(private val db: Database, private val veilarbarenaClient: VeilarbarenaClient) {
    val log: Logger = LoggerFactory.getLogger(HistorikkService::class.java)

    suspend fun hentHistorikkForBruker(fnr: String, accessToken: String?): List<HistorikkForDeltaker> {
        val personId = veilarbarenaClient.hentPersonIdForFnr(fnr, accessToken) ?: run {
            log.info("Klarte ikke hente personId fra veilarbarena")
            return emptyList()
        }
        return getHistorikkForBrukerFromDb(parseInt(personId, 10))
    }

    private fun getHistorikkForBrukerFromDb(person_id: Int): List<HistorikkForDeltaker> {
        @Language("PostgreSQL")
        val query = """
        select deltaker.id, deltaker.fra_dato, deltaker.til_dato, status, gjennomforing.navn, tiltaksnummer, tiltakstype.navn tiltakstype
        from deltaker
        left join tiltaksgjennomforing gjennomforing on gjennomforing.arena_id = deltaker.tiltaksgjennomforing_id
        left join tiltakstype tiltakstype on tiltakstype.tiltakskode = gjennomforing.tiltakskode
        where person_id = ?
        order by deltaker.fra_dato desc nulls last;
        """.trimIndent()
        val queryResult = queryOf(query, person_id).map { DatabaseMapper.toBrukerHistorikk(it) }.asList
        return db.run(queryResult)
    }
}
