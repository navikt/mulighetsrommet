package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.NotFound
import no.nav.mulighetsrommet.api.responses.ServerError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.koin.ktor.ext.inject

fun Route.brregVirksomhetRoutes() {
    val arrangorRepository: ArrangorRepository by inject()
    val brregClient: BrregClient by inject()

    route("virksomhet") {
        get("sok") {
            val sok: String by call.request.queryParameters

            if (sok.isBlank()) {
                throw BadRequestException("'sok' kan ikke være en tom streng")
            }

            val response = brregClient.sokOverordnetEnhet(sok)
                .map { hovedenheter ->
                    // Kombinerer resultat med utenlandske virksomheter siden de ikke finnes i brreg
                    hovedenheter + arrangorRepository.getAll(sok = sok, utenlandsk = true).items.map { virksomhet ->
                        toBrregVirksomhetDto(virksomhet)
                    }
                }
                .mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
        }

        get("{orgnr}/underenheter") {
            val orgnr = call.parameters.getOrFail<Organisasjonsnummer>("orgnr")

            val response = brregClient.getUnderenheterForOverordnetEnhet(orgnr)
                .map { underenheter ->
                    // Kombinerer resultat med virksomheter som er slettet fra brreg for å støtte avtaler/gjennomføringer som henger etter
                    underenheter + arrangorRepository.getAll(overordnetEnhetOrgnr = orgnr, slettet = true).items
                        .map { virksomhet -> toBrregVirksomhetDto(virksomhet) }
                }
                .mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
        }
    }
}

fun isUtenlandskOrgnr(orgnr: Organisasjonsnummer): Boolean {
    return orgnr.value.matches("^[1-7][0-9]{8}\$".toRegex())
}

fun toStatusResponseError(it: BrregError) = when (it) {
    BrregError.NotFound -> NotFound()
    BrregError.BadRequest -> BadRequest()
    BrregError.Error -> ServerError()
}

private fun toBrregVirksomhetDto(arrangor: ArrangorDto) = BrregVirksomhetDto(
    organisasjonsnummer = arrangor.organisasjonsnummer,
    navn = arrangor.navn,
    overordnetEnhet = arrangor.overordnetEnhet,
    underenheter = listOf(),
    postnummer = arrangor.postnummer,
    poststed = arrangor.poststed,
    slettetDato = arrangor.slettetDato,
)
