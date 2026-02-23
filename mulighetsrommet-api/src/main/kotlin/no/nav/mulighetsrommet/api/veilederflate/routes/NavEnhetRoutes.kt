package no.nav.mulighetsrommet.api.veilederflate.routes

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.regionRoutes() {
    val navEnhetService: NavEnhetService by inject()

    get("nav-enheter/kontorstruktur", {
        tags = setOf("NavEnheter")
        operationId = "getKontorstruktur"
        response {
            code(HttpStatusCode.OK) {
                description = "Alle Nav-enheter"
                body<List<Kontorstruktur>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        call.respond(navEnhetService.hentKontorstruktur())
    }

    // TODO: skal fjernes, midlertidig inkludert for å være bakoverkompatibel
    get("nav-enheter/regioner", {
        tags = setOf("NavEnheter")
        operationId = "getRegioner"
        response {
            code(HttpStatusCode.OK) {
                description = "Alle Nav-enheter"
                body<List<NavRegionDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        call.respond(navEnhetService.hentRegioner())
    }
}
