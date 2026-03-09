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
            code(HttpStatusCode.NotFound) {
                description = "Hovedenheter ble ikke funnet"
            }
        }
    }) {
        val term: String by call.parameters
        val result = arrangorService.brregSok(term)
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
            code(HttpStatusCode.NotFound) {
                description = "Underenheter ble ikke funnet"
            }
        }
    }) {
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        val result = arrangorService.brregUnderenheter(orgnr)
        call.respondWithStatusResponse(result)
    }
}
