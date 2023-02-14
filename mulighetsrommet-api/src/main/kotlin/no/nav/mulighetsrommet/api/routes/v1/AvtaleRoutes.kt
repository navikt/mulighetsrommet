package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.getTiltakstypeFilter
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()

    route("/api/v1/internal/avtaler") {
        get {
            val pagination = getPaginationParams()

            val result = avtaler.getAll(pagination)

            call.respond(result)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                text = "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest,
            )

            val avtale = avtaler.get(id) ?: return@get call.respondText(
                text = "Det finnes ikke noen avtale med id $id",
                status = HttpStatusCode.NotFound,
            )

            call.respond(avtale)
        }

        get("tiltakstype/{tiltakstypeId}") {
            val pagination = getPaginationParams()
            getTiltakstypeFilter()
            val id = call.parameters["tiltakstypeId"]?.toUUID() ?: return@get call.respondText(
                text = "Mangler eller ugyldig id for tiltakstype",
                status = HttpStatusCode.BadRequest,
            )

            call.respond(avtaler.getAvtalerForTiltakstype(id, pagination))
        }
    }
}
