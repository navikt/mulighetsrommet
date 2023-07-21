package no.nav.mulighetsrommet.api.routes.api.v1.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.getTiltakstypeFilter
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()

    route("/api/v1/internal/tiltakstyper") {
        get {
            val filter = getTiltakstypeFilter()
            val paginationParams = getPaginationParams()

            call.respond(
                tiltakstypeService.getWithFilter(
                    filter,
                    paginationParams,
                ),
            )
        }
        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            call.respond(tiltakstype)
        }

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")
            val nokkeltall = tiltakstypeService.getNokkeltallForTiltakstype(id)

            call.respond(nokkeltall)
        }
    }
}
