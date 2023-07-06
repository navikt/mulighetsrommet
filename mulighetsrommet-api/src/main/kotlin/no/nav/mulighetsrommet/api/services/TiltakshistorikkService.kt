package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val arrangorService: ArrangorService,
    private val tiltakshistorikkRepository: TiltakshistorikkRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikkForBruker(norskIdent: String): List<TiltakshistorikkDto> {
        return tiltakshistorikkRepository.getTiltakshistorikkForBruker(norskIdent).map {
            val arrangor = it.arrangorOrganisasjonsnummer?.let { orgnr ->
                val navn = hentArrangorNavn(orgnr)
                TiltakshistorikkDto.Arrangor(organisasjonsnummer = orgnr, navn = navn)
            }
            it.run {
                TiltakshistorikkDto(
                    id = id,
                    fraDato = fraDato,
                    tilDato = tilDato,
                    status = status,
                    tiltaksnavn = tiltaksnavn,
                    tiltakstype = tiltakstype,
                    arrangor = arrangor,
                )
            }
        }
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
