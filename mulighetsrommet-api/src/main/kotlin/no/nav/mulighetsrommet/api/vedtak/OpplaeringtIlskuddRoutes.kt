package no.nav.mulighetsrommet.api.vedtak

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.opplaeringTilskuddRoutes() {
    val db: ApiDatabase by inject()

    route("/opplaeringtilskudd") {
        get({
            description = "Hent alle opplaeringtilskudd"
            tags = setOf("Opplaeringtilskudd")
            operationId = "getAll"
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle tilskudd for opplaering"
                    body<List<Opplaeringtilskudd>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tilskudd = db.session {
                queries.opplaeringtilskudd.getAll()
            }
            call.respond(tilskudd)
        }
    }
}
