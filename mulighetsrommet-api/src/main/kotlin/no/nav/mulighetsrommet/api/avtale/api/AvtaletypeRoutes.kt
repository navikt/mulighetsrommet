package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.avtale.model.AvtaletypeInfo
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode

fun Route.avtaletypeRoutes() {
    route("avtaletyper") {
        get({
            tags = setOf("Avtaletype")
            operationId = "getAvtaletyper"
            request {
                queryParameter<Tiltakskode>("tiltakstype")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtaletyper for tiltakstype"
                    body<List<AvtaletypeInfo>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tiltakstype: Tiltakskode by call.queryParameters

            val avtaletyper = Avtaletyper
                .getAvtaletyperForTiltak(tiltakstype)
                .map { AvtaletypeInfo(type = it, tittel = it.tittel) }

            call.respond(avtaletyper)
        }
    }
}
