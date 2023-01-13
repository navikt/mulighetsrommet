package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.domain.models.TiltakshistorikkDTO
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HistorikkService(
    private val arrangorService: ArrangorService,
    private val tiltakshistorikkRepository: TiltakshistorikkRepository
) {
    val log: Logger = LoggerFactory.getLogger(HistorikkService::class.java)

    suspend fun hentHistorikkForBruker(norskIdent: String): List<TiltakshistorikkDTO> {
        return tiltakshistorikkRepository.getTiltakshistorikkForBruker(norskIdent).map {
            val arrangor = it.arrangor?.let { virksomhetsnummer -> hentArrangorNavn(virksomhetsnummer) }
            it.copy(arrangor = arrangor)
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
}
