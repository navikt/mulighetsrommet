package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.api.avtale.model.AvtaletypeInfo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellInfo
import no.nav.mulighetsrommet.api.avtale.model.Prismodeller
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode

fun Route.kodeverkRoutes() {
    route("kodeverk") {
        get("avtaletyper", {
            tags = setOf("Kodeverk")
            operationId = "getAvtaletyper"
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtaletyper per tiltakskode"
                    body<Map<Tiltakskode, List<AvtaletypeInfo>>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val avtaletyper = Tiltakskode.entries.associateWith { tiltakskode ->
                Avtaletyper
                    .getAvtaletyperForTiltak(tiltakskode)
                    .map { AvtaletypeInfo(type = it, tittel = it.tittel) }
            }
            call.respond(avtaletyper)
        }

        get("prismodeller", {
            tags = setOf("Kodeverk")
            operationId = "getPrismodeller"
            response {
                code(HttpStatusCode.OK) {
                    description = "Prismodeller per tiltakskode"
                    body<Map<Tiltakskode, List<PrismodellInfo>>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val prismodeller = Tiltakskode.entries.associateWith { tiltakskode ->
                Prismodeller
                    .getPrismodellerForTiltak(tiltakskode)
                    .map { PrismodellInfo(type = it, navn = it.navn, beskrivelse = it.beskrivelse) }
            }
            call.respond(prismodeller)
        }
    }
}
