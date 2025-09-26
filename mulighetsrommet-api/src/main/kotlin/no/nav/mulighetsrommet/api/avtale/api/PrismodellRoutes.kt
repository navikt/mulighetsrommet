package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.avtale.model.PrismodellInfo
import no.nav.mulighetsrommet.api.avtale.model.Prismodeller
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode

fun Route.prismodellRoutes() {
    route("prismodeller") {
        get({
            tags = setOf("Prismodell")
            operationId = "getPrismodeller"
            request {
                queryParameter<Tiltakskode>("tiltakstype")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Prismodeller for tiltakstype"
                    body<List<PrismodellInfo>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tiltakstype: Tiltakskode by call.queryParameters

            val prismodeller = Prismodeller.getPrismodellerForTiltak(tiltakstype)
                .map { PrismodellInfo(type = it, beskrivelse = it.beskrivelse) }

            call.respond(prismodeller)
        }
    }
}
