package no.nav.mulighetsrommet.api.navenhet

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.api.kostnadssted.KostnadsstedService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val kostnadsstedService: KostnadsstedService by inject()
    val navEnhetService: NavEnhetService by inject()

    route("nav-enheter") {
        get("regioner", {
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

        get("kostnadsstedFilter", {
            tags = setOf("NavEnheter")
            operationId = "getKostnadsstedFilter"
            response {
                code(HttpStatusCode.OK) {
                    description = "Filtre for kostnadssteder"
                    body<List<NavRegionDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(kostnadsstedService.hentKostnadsstedFilter())
        }
    }
}

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<NavEnhetType>? = null,
    val overordnetEnhet: NavEnhetNummer? = null,
)
