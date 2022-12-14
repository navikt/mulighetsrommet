package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get {
            val paginationParams = getPaginationParams()
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                paginationParams
            )
            call.respond(
                PaginatedResponse(
                    pagination = Pagination(
                        totalCount = totalCount,
                        currentPage = paginationParams.page,
                        pageSize = paginationParams.limit
                    ),
                    data = items
                )
            )
        }

        get("tiltakskode/{tiltakskode}") {
            val tiltakskode = call.parameters["tiltakskode"] ?: return@get call.respondText(
                "Mangler eller ugyldig tiltakskode",
                status = HttpStatusCode.BadRequest
            )

            val paginationParams = getPaginationParams()
            val (totalCount, items) = tiltaksgjennomforinger.getAllByTiltakskode(
                tiltakskode,
                paginationParams
            )
            call.respond(
                PaginatedResponse(
                    pagination = Pagination(
                        totalCount = totalCount,
                        currentPage = paginationParams.page,
                        pageSize = paginationParams.limit
                    ),
                    data = items
                )
            )
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing =
                tiltaksgjennomforinger.get(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltaksgjennomforing)
        }

        get("tiltakstypedata/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing =
                tiltaksgjennomforinger.getWithTiltakstypedata(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltaksgjennomforing)
        }

        get("sok") {
            val tiltaksnummer = call.request.queryParameters.get("tiltaksnummer") ?: return@get call.respondText(
                "Mangler query-param 'tiltaksnummer'",
                status = HttpStatusCode.BadRequest
            )

            val gjennomforinger = tiltaksgjennomforingService.sok(Sokefilter(tiltaksnummer = tiltaksnummer))
            if (gjennomforinger.isEmpty()) {
                call.respond(status = HttpStatusCode.NoContent, "Fant ingen tiltaksgjennomføringer for søket")
            }

            call.respond(gjennomforinger)
        }
    }
}
