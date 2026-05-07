package no.nav.mulighetsrommet.api.janzz.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.janzz.PamOntologiService
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.janzzRoutes() {
    val pamService: PamOntologiService by inject()

    get("janzz/sertifiseringer/sok", {
        description = "Søk etter sertifiseringer fra Janzz"
        tags = setOf("Janzz")
        operationId = "sokSertifiseringer"
        request {
            queryParameter<String>("q") {
                description = "Søketekst for sertifisering"
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste over sertifiseringer som matcher søket"
                body<List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val q: String by call.request.queryParameters

        val sertifiseringer = pamService.sokSertifiseringer(q)

        call.respond(sertifiseringer)
    }
}
