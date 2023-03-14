package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.utils.getAvtaleFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()

    route("/api/v1/internal/avtaler") {
        get {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()
            val result = avtaler.getAll(filter, pagination)

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

        get("{id}/nokkeltall") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                text = "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest,
            )

            val nokkeltall = avtaler.getNokkeltallForAvtaleMedId(id)

            call.respond(nokkeltall)
        }
    }
}
