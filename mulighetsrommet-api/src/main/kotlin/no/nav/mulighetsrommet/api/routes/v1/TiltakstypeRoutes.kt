package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.getTiltakstypeFilter
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()

    route("/api/v1/internal/tiltakstyper") {
        authenticate(AuthProvider.AzureAdNavIdent.name) {
            get {
                val filter = getTiltakstypeFilter()
                val paginationParams = getPaginationParams()

                call.respond(
                    tiltakstypeService.getWithFilter(
                        filter,
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

            get("/tags") {
                call.respond(tiltakstypeService.getAlleTagsForTiltakstype())
            }
        }

        authenticate(AuthProvider.AzureAdFagansvarlig.name) {
            put("{id}/tags") {
                val id = call.parameters["id"]?.toUUID() ?: return@put call.respondText(
                    "Mangler eller ugyldig id",
                    status = HttpStatusCode.BadRequest
                )
                val tags = call.receive<Set<String>>()
                val tiltakstype = tiltakstypeService.lagreTags(tags, id) ?: return@put call.respondText(
                    "Det finnes ikke noe tiltakstype med id $id ved opprettelse av tags",
                    status = HttpStatusCode.NotFound
                )
                call.respond(tiltakstype)
            }
        }
    }
}
