package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.toUUID
import org.koin.ktor.ext.inject

// TODO: Må lage noe felles validering her etterhvert
fun Parameters.parseList(parameter: String): List<String> {
    return entries().filter { it.key == parameter }.flatMap { it.value }
}

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/tiltakstyper") {
        get() {
            val search = call.request.queryParameters["search"]

//            val innsatsgrupper = call.request.queryParameters.parseList("innsatsgrupper").map { Integer.parseInt(it) }

            val paginationParams = getPaginationParams()

            val (totalCount, items) = tiltakstypeService.getTiltakstyper(search, paginationParams)

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
            val tiltakstype =
                tiltakstypeService.getTiltakstypeById(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltakstype med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltakstype)
        }
        get("{id}/tiltaksgjennomforinger") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforinger =
                tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakstypeId(id)

            call.respond(tiltaksgjennomforinger)
        }
    }
}
