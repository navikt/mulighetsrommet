package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.routes.v1.responses.Links
import no.nav.mulighetsrommet.api.routes.v1.responses.ListResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.koin.ktor.ext.inject

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get() {
            call.respond(
                TiltaksgjennomforingerResponse(
                    data = tiltaksgjennomforingService.getTiltaksgjennomforinger()
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
                    "Det finner ikke noe tiltak med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltaksgjennomforing)
        }
    }
}

@Serializable
data class TiltaksgjennomforingerResponse(
    override val data: List<Tiltaksgjennomforing>,
    override val pagination: Pagination? = null,
    override val links: Links? = null
) : ListResponse
