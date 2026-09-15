package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.Tilskudd
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilskuddRoutes() {
    val db: ApiDatabase by inject()

    route("tilskudd") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            get("/{tilskuddId}", {
                description = "Hent tilskudds gitt id"
                tags = setOf("Tilskudd")
                operationId = "getTilskudd"
                request {
                    pathParameterUuid("tilskuddId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskudd"
                        body<Tilskudd>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tilskuddId = call.parameters.getOrFail<UUID>("tilskuddId")
                val result = db.session { queries.tilskudd.get(tilskuddId) }
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(result)
            }
        }
    }
}
