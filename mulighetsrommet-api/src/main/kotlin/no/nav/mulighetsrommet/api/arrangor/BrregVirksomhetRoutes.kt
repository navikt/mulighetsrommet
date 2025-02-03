package no.nav.mulighetsrommet.api.arrangor

import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.koin.ktor.ext.inject

fun Route.brregVirksomhetRoutes() {
    val db: ApiDatabase by inject()
    val brregClient: BrregClient by inject()

    route("virksomhet") {
        get("sok") {
            val sok: String by call.request.queryParameters

            if (sok.isBlank()) {
                throw BadRequestException("'sok' kan ikke være en tom streng")
            }

            val response = brregClient.sokHovedenhet(sok)
                .map { hovedenheter ->
                    val utenlandskeVirksomheter = db.session {
                        queries.arrangor.getAll(sok = sok, utenlandsk = true).items.map {
                            toBrregVirksomhetDto(it)
                        }
                    }
                    // Kombinerer resultat med utenlandske virksomheter siden de ikke finnes i brreg
                    hovedenheter + utenlandskeVirksomheter
                }
                .mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
        }

        get("{orgnr}/underenheter") {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            val response = brregClient.getUnderenheterForHovedenhet(orgnr)
                .map { underenheter ->
                    val slettedeVirksomheter = db.session {
                        queries.arrangor.getAll(overordnetEnhetOrgnr = orgnr, slettet = true).items.map {
                            toBrregVirksomhetDto(it)
                        }
                    }
                    // Kombinerer resultat med virksomheter som er slettet fra brreg for å støtte avtaler/gjennomføringer som henger etter
                    underenheter + slettedeVirksomheter
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
    BrregError.NotFound -> NotFound("not found fra brreg")
    BrregError.BadRequest -> BadRequest("bad request mot brreg")
    BrregError.Error -> InternalServerError("brreg internal server error")
}

private fun toBrregVirksomhetDto(arrangor: ArrangorDto) = when {
    arrangor.slettetDato != null -> SlettetBrregHovedenhetDto(
        organisasjonsnummer = arrangor.organisasjonsnummer,
        organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
        navn = arrangor.navn,
        slettetDato = arrangor.slettetDato,
    )

    else -> BrregHovedenhetDto(
        organisasjonsnummer = arrangor.organisasjonsnummer,
        organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
        navn = arrangor.navn,
        postadresse = null,
    )
}
