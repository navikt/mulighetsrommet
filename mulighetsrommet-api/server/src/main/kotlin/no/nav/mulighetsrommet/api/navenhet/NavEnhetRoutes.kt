package no.nav.mulighetsrommet.api.navenhet

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.api.kostnadssted.KostnadsstedService
import no.nav.mulighetsrommet.api.kostnadssted.RegionKostnadssteder
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val kostnadsstedService: KostnadsstedService by inject()
    val navEnhetService: NavEnhetService by inject()

    route("kodeverk") {
        get("kontorstruktur", {
            tags = setOf("Kodeverk")
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

        get("kostnadssteder", {
            tags = setOf("Kodeverk")
            operationId = "getKostnadssteder"
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle kostnadssteder"
                    body<List<RegionKostnadssteder>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(kostnadsstedService.hentKostnadssteder())
        }
    }
}

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<NavEnhetType>? = null,
    val overordnetEnhet: NavEnhetNummer? = null,
)
