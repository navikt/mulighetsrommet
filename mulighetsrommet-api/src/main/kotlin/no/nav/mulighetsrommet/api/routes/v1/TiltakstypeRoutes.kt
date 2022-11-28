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

            val innsatsgrupper = call.request.queryParameters.parseList("innsatsgrupper").map { Integer.parseInt(it) }

            val paginationParams = getPaginationParams()

            val (totalCount, items) = tiltakstypeService.getTiltakstyper(innsatsgrupper, search, paginationParams)

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
            runCatching {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                    "Mangler eller ugyldig id",
                    status = HttpStatusCode.BadRequest
                )
                tiltakstypeService.getTiltakstypeById(id)
            }.onSuccess { fetchedTiltakstype ->
                if (fetchedTiltakstype != null) {
                    call.respond(fetchedTiltakstype)
                }
                call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
            }.onFailure {
                call.application.environment.log.error(it.stackTraceToString())
                call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
            }
        }

        get("{tiltakskode}") {
            runCatching {
                val tiltakskode = call.parameters["tiltakskode"]!!
                tiltakstypeService.getTiltakstypeByTiltakskode(tiltakskode)
            }.onSuccess { fetchedTiltakstype ->
                if (fetchedTiltakstype != null) {
                    call.respond(fetchedTiltakstype)
                }
                call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
            }.onFailure {
                call.application.environment.log.error(it.stackTraceToString())
                call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
            }
        }
        get("{tiltakskode}/tiltaksgjennomforinger") {
            runCatching {
                val tiltakskode = call.parameters["tiltakskode"]!!
                tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakskode(tiltakskode)
            }.onSuccess { fetchedTiltaksgjennomforinger ->
                call.respond(fetchedTiltaksgjennomforinger)
            }.onFailure { call.respondText("Fant ikke tiltakstype", status = HttpStatusCode.NotFound) }
        }
    }
}
