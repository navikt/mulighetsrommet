package no.nav.mulighetsrommet.tiltak.okonomi

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsTiltakApiClient

private const val API_BASE_PATH = "/api/v1/okonomi"

@Resource("$API_BASE_PATH/bestilling")
class Bestilling {

    @Resource("{id}")
    class Id(val parent: Bestilling = Bestilling(), val id: String)
}

fun Application.okonomiRoutes(
    oebs: OebsTiltakApiClient,
) = routing {
    install(Resources)

    authenticate {
        get<Bestilling.Id> {
            val id: String by call.parameters

            val response = oebs.getBestillingStatus(id)

            call.respond(response)
        }
    }
}
