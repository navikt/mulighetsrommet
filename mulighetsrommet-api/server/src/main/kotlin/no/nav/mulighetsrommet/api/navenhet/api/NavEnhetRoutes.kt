package no.nav.mulighetsrommet.api.navenhet.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQuery
import no.nav.mulighetsrommet.admin.kostnadssted.RegionKostnadssteder
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.KontorstrukturQuery
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val kostnadsstedQuery: KostnadsstedQuery by inject()
    val kontorstrukturQuery: KontorstrukturQuery by inject()

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
            call.respond(kontorstrukturQuery.execute())
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
            call.respond(kostnadsstedQuery.execute())
        }
    }
}
