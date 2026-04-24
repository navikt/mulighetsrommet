package no.nav.mulighetsrommet.api.endringshistorikk.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.endringshistorikkRoutes() {
    val db: ApiDatabase by inject()
    get("historikk/{id}", {
        tags = setOf("Endringshistorikk")
        operationId = "getEndringshistorikk"
        request {
            pathParameterUuid("id")
            queryParameter<DocumentClass>("documentClass")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Endringshistorikk"
                body<EndringshistorikkDto>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val id: UUID by call.parameters
        val documentClass: DocumentClass by call.request.queryParameters

        val historikk = db.session { queries.endringshistorikk.getEndringshistorikk(documentClass, id) }
        call.respond(historikk)
    }
}
