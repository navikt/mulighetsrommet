package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.personopplysningRoutes() {
    val db: ApiDatabase by inject()

    route("personopplysninger") {
        get({
            tags = setOf("Personopplysning")
            operationId = "getPersonopplysninger"
            response {
                code(HttpStatusCode.OK) {
                    description = "Kodeverk over typer personopplysninger som kan deles mellom Nav og arrangør"
                    body<List<Personopplysning>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(
                db.session { queries.avtale.getPersonopplysninger() }
                    .sortedBy { it.sortKey },
            )
        }
    }
}
