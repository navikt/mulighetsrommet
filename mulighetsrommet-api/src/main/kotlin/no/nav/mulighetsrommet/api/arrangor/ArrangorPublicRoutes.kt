package no.nav.mulighetsrommet.api.arrangor

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.arrangorPublicRoutes() {
    route("/v1/arrangor") {
        enhetRoutes()
    }
}

private fun Route.enhetRoutes() {
    val arrangorService: ArrangorService by inject()

    get("/hovedenhet/sok/{term}", {
        tags = setOf("")
        operationId = "sokHovedenhet"
        request {
            pathParameter<String>("term")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste av hovedenheter"
                body<List<BrregHovedenhetDto>>()
            }
            code(HttpStatusCode.BadRequest) {
                description = "Søket er blankt"
                body<ProblemDetail>()
            }
            code(HttpStatusCode.InternalServerError) {
                description = "Internal server error ved kall mot Brreg"
                body<ProblemDetail>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val term: String by call.parameters
        val result = arrangorService.brregSok(term)
            .mapLeft { it.toProblemDetail() }
        call.respondWithStatusResponse(result)
    }

    get("/hovedenhet/{orgnr}/underenheter", {
        tags = setOf("")
        operationId = "hentUnderenheter"
        request {
            pathParameter<Organisasjonsnummer>("orgnr")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste av underenheter"
                body<List<BrregUnderenhetDto>>()
            }
            code(HttpStatusCode.BadRequest) {
                description = "Feil format på organisasjonsnummer"
            }
            code(HttpStatusCode.InternalServerError) {
                description = "Feil oppstod ved henting av underenheter fra Brreg"
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        val result = arrangorService.brregUnderenheter(orgnr)
            .mapLeft { it.toProblemDetail(orgnr) }
        call.respondWithStatusResponse(result)
    }
}
