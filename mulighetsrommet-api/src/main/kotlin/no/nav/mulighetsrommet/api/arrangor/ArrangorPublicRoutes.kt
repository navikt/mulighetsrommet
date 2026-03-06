package no.nav.mulighetsrommet.api.arrangor

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.brreg.BrregHovedenhet
import no.nav.mulighetsrommet.brreg.BrregUnderenhet
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
                body<List<BrregHovedenhet>>()
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
                body<List<BrregUnderenhet>>()
            }
            code(HttpStatusCode.NotFound) {
                description = "Underenheter ble ikke funnet"
            }
        }
    }) {
        val orgnr: Organisasjonsnummer by call.parameters
        val result = arrangorService.brregUnderenheter(orgnr)
        call.respondWithStatusResponse(result)
    }
}
