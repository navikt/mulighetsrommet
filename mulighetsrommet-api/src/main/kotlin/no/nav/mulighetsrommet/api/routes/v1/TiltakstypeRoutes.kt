package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()

    route("/api/v1/internal/tiltakstyper") {
        get {
            val search = call.request.queryParameters["search"]
            val status =
                call.request.queryParameters["status"]?.let { status -> Status.valueOf(status) }

            val paginationParams = getPaginationParams()

            if (status != null) {
                call.respond(
                    tiltakstypeService.getWithFilter(
                        TiltakstypeFilter(search = search, status = status),
                        paginationParams
                    )
                )
            }

            call.respond(
                tiltakstypeService.getAll(
                    paginationParams
                )
            )
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound
            )

            call.respond(tiltakstype)
        }
    }
}

data class TiltakstypeFilter(
    val search: String?,
    val status: Status
)

enum class Status {
    AKTIV, PLANLAGT, AVSLUTTET
}
