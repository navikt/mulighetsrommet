package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.tiltakstypeRoutes() {
    val tiltakstyper: TiltakstypeRepository by inject()

    route("/api/v1/internal/tiltakstyper") {
        get {
            val search = call.request.queryParameters["search"]

            val paginationParams = getPaginationParams()

            val (totalCount, items) = tiltakstyper.getAll(search, paginationParams)

            call.respond(
                PaginatedResponse(
                    data = items,
                    pagination = Pagination(
                        totalCount = totalCount,
                        currentPage = paginationParams.page,
                        pageSize = paginationParams.limit
                    )
                )
            )
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltakstype = tiltakstyper.get(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound
            )

            call.respond(tiltakstype)
        }
    }
}
