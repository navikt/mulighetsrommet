package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserResponse
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val arrangorService: ArrangorService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkRepository: TiltakshistorikkRepository,
    private val pdlClient: PdlClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikkForBruker(norskIdent: String, obo: AccessType.OBO): List<TiltakshistorikkDto> {
        val identer = pdlClient.hentIdenter(norskIdent, obo)
            .map { list -> list.map { it.ident } }
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw Exception("Feil mot pdl!")
                    PdlError.NotFound -> listOf(norskIdent)
                }
            }

        return tiltakshistorikkRepository.getTiltakshistorikkForBruker(identer).map {
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

    suspend fun hentDeltakelserFraKomet(
        norskIdent: String,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        return amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), obo)
    }

    fun slettHistorikkForIdenter(identer: List<String>) =
        tiltakshistorikkRepository.deleteTiltakshistorikkForIdenter(identer)

    private suspend fun hentArrangorNavn(orgnr: String): String? {
        return arrangorService.getOrSyncArrangorFromBrreg(orgnr).fold({ error ->
            log.warn("Klarte ikke hente arrangør. BrregError: $error")
            null
        }, { virksomhet ->
            virksomhet.navn
        })
    }
}
