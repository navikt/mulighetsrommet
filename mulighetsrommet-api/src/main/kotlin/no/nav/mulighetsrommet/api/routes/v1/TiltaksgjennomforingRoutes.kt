package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import org.koin.ktor.ext.inject

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get() {
            val paginationParams = getPaginationParams()
            val (totalCount, tiltaksgjennomforinger) = tiltaksgjennomforingService.getTiltaksgjennomforinger(
                paginationParams
            )
            call.respond(
                PaginatedResponse(
                    pagination = Pagination(
                        totalCount = totalCount,
                        currentPage = paginationParams.page,
                        pageSize = paginationParams.limit
                    ),
                    data = tiltaksgjennomforinger
                )
            )
        }
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing =
                tiltaksgjennomforingService.getTiltaksgjennomforingById(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltak med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltaksgjennomforing)
        }
    }
}
