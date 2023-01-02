package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
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
            val (totalCount, items) = tiltaksgjennomforingService.getAll(paginationParams)
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
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig tiltakstypeId",
                status = HttpStatusCode.BadRequest
            )

            val paginationParams = getPaginationParams()
            val (totalCount, items) = tiltaksgjennomforinger.getAllByTiltakstypeId(id, paginationParams)
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
            val tiltaksgjennomforing = tiltaksgjennomforinger.get(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(tiltaksgjennomforing)
        }

        get("enhet/{enhet}") {
            val enhet = call.parameters["enhet"] ?: return@get call.respondText(
                "Mangler enhet",
                status = HttpStatusCode.BadRequest
            )
            val paginationParams = getPaginationParams()

            val (totalCount, items) = tiltaksgjennomforingService.getAllByEnhet(
                enhet,
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

        get("mine") {
            val navIdent = getNavIdent()
            val paginationParams = getPaginationParams()

            val (totalCount, items) = tiltaksgjennomforingService.getAllForAnsatt(
                navIdent,
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

        get("sok") {
            val tiltaksnummer = call.request.queryParameters["tiltaksnummer"] ?: return@get call.respondText(
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
