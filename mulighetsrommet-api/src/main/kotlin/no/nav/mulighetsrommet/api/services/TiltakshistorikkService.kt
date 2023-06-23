package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.teamtiltak.TeamTiltakClient
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val arrangorService: ArrangorService,
    private val tiltakshistorikkRepository: TiltakshistorikkRepository,
    private val teamTiltakClient: TeamTiltakClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikkForBruker(norskIdent: String): List<TiltakshistorikkDto> {
        val jsonTeamTiltak = teamTiltakClient.getAvtaler(norskIdent)
        log.warn("json fra team tiltak: $jsonTeamTiltak")
        val historikk = tiltakshistorikkRepository.getTiltakshistorikkForBruker(norskIdent).map {
            val arrangor = it.arrangor?.let { arrangor ->
                val navn = hentArrangorNavn(arrangor.virksomhetsnummer)
                arrangor.copy(navn = navn)
            }
            it.copy(arrangor = arrangor)
        }
        return historikk
    }

    private suspend fun hentArrangorNavn(virksomhetsnummer: String): String? {
        return try {
            arrangorService.hentOverordnetEnhetNavnForArrangor(virksomhetsnummer)
        } catch (e: Throwable) {
            log.error("Feil oppstod ved henting arrangørnavn, sjekk securelogs")
            SecureLog.logger.error("Feil oppstod ved henting arrangørnavn", e)
            null
        }
    }
}
