package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val virksomhetService: VirksomhetService,
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
        return virksomhetService.getOrSyncHovedenhetFromBrreg(virksomhetsnummer).fold({ error ->
            log.warn("Klarte ikke hente arrangør. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })
    }
}
