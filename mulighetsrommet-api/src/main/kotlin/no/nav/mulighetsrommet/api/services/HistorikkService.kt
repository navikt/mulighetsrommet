package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltaker
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltakerDTO
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HistorikkService(
    private val db: Database,
    private val arrangorService: ArrangorService
) {
    val log: Logger = LoggerFactory.getLogger(HistorikkService::class.java)

    suspend fun hentHistorikkForBruker(norskIdent: String): List<HistorikkForDeltakerDTO> {
        return getHistorikkForBrukerFromDb(norskIdent).map {
            val arrangor = it.virksomhetsnummer?.let { virksomhetsnummer -> hentArrangorNavn(virksomhetsnummer) }
            HistorikkForDeltakerDTO(
                id = it.id,
                fraDato = it.fraDato,
                tilDato = it.tilDato,
                status = it.status,
                tiltaksnavn = it.tiltaksnavn,
                tiltaksnummer = it.tiltaksnummer,
                tiltakstype = it.tiltakstype,
                arrangor = arrangor
            )
        }
    }

    private suspend fun hentArrangorNavn(virksomhetsnummer: String): String? {
        return try {
            arrangorService.hentArrangornavn(virksomhetsnummer)
        } catch (e: Throwable) {
            log.error("Feil oppstod ved henting arrangørnavn, sjekk securelogs")
            SecureLog.logger.error("Feil oppstod ved henting arrangørnavn", e)
            null
        }
    }

    private fun getHistorikkForBrukerFromDb(norskIdent: String): List<HistorikkForDeltaker> {
        @Language("PostgreSQL")
        val query = """
            select deltaker.id,
                   deltaker.fra_dato,
                   deltaker.til_dato,
                   deltaker.status,
                   gjennomforing.navn,
                   gjennomforing.virksomhetsnummer,
                   gjennomforing.tiltaksnummer,
                   tiltakstype.navn as tiltakstype
            from deltaker
                     left join tiltaksgjennomforing gjennomforing on gjennomforing.id = deltaker.tiltaksgjennomforing_id
                     left join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
            where norsk_ident = ?
            order by deltaker.fra_dato desc nulls last;
        """.trimIndent()
        val queryResult = queryOf(query, norskIdent).map { it.toHistorikkForDeltaker() }.asList
        return db.run(queryResult)
    }

    private fun Row.toHistorikkForDeltaker(): HistorikkForDeltaker = HistorikkForDeltaker(
        id = uuid("id"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(string("status")),
        tiltaksnavn = stringOrNull("navn"),
        tiltaksnummer = string("tiltaksnummer"),
        tiltakstype = string("tiltakstype"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer")
    )
}
