package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
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

        get("tiltakstype/{id}") {
            val tiltakstypeId = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig tiltakstypeId",
                status = HttpStatusCode.BadRequest
            )

            val paginationParams = getPaginationParams()
            val (totalCount, items) = tiltaksgjennomforinger.getAllByTiltakstypeId(tiltakstypeId, paginationParams)
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

            val (totalCount, items) = tiltaksgjennomforingService.getAllForAnsattsListe(
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

        post("mine") {
            val navIdent = getNavIdent()
            val tiltaksgjennomforingId = call.receive<String>()

            tiltaksgjennomforingService.lagreGjennomforingTilAnsattsListe(UUID.fromString(tiltaksgjennomforingId), navIdent)
            call.respond(
                HttpStatusCode.OK
            )
        }

        delete("mine") {
            val navIdent = getNavIdent()
            val tiltaksgjennomforingId = call.receive<String>()

            tiltaksgjennomforingService.fjernGjennomforingFraAnsattsListe(UUID.fromString(tiltaksgjennomforingId), navIdent)
            call.respond(
                HttpStatusCode.OK
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

        get("{id}/nokkeltall") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val nokkeltall = tiltaksgjennomforingService.getNokkeltallForTiltaksgjennomforing(id)

            call.respond(nokkeltall)
        }
    }
}

