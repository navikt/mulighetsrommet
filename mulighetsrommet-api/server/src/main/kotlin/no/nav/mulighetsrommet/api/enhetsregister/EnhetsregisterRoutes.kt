package no.nav.mulighetsrommet.api.enhetsregister

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterQuery
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.api.arrangor.toProblemDetail
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.enhetsregisterRoutes() {
    val enhetsregister: EnhetsregisterQuery by inject()

    route("virksomhet") {
        get("hovedenhet", {
            tags = setOf("Virksomhet")
            operationId = "sokHovedenheter"
            description = "Søk etter hovedenhet på orgnr eller navn"
            request {
                queryParameter<String>("sok") {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste med hovedenheter"
                    body<List<Hovedenhet>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val sok: String by call.request.queryParameters
            val result = enhetsregister.sokHovedenheter(sok).mapLeft { it.toProblemDetail() }
            call.respondWithStatusResponse(result)
        }

        get("underenhet", {
            tags = setOf("Virksomhet")
            operationId = "sokUnderenhet"
            description = "Søk etter underenhet på orgnr eller navn"
            request {
                queryParameter<String>("sok") {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste med underenheter"
                    body<List<Underenhet>>()
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
            val result = enhetsregister.sokUnderenheter(sok).mapLeft { it.toProblemDetail() }
            call.respondWithStatusResponse(result)
        }

        get("{orgnr}/underenheter", {
            tags = setOf("Virksomhet")
            operationId = "getUnderenheter"
            request {
                pathParameter<String>("orgnr")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Underenhetene til hovedenhet for gitt orgnr"
                    body<List<Underenhet>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            val result = enhetsregister.hentUnderenheterForHovedenhet(orgnr).mapLeft { it.toProblemDetail() }
            call.respondWithStatusResponse(result)
        }
    }
}
