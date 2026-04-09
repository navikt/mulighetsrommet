package no.nav.mulighetsrommet.api.arrangor

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.arrangorPublicRoutes() {
    route("/v1/arrangor") {
        enhetRoutes()
    }
}

private fun Route.enhetRoutes() {
    val arrangorService: ArrangorService by inject()

    get("/underenhet", {
        tags = setOf("Arrangor")
        operationId = "sokUnderenhet"
        description = "Søk etter unrderenhet på orgnr eller navn"
        request {
            queryParameter<String>("sok") {
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste med underenheter"
                body<List<BrregUnderenhetDto>>()
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
        val sok: String by call.parameters
        val result = arrangorService.brregSokUnderenheter(sok).mapLeft { it.toProblemDetail() }
        call.respondWithStatusResponse(result)
    }
}
