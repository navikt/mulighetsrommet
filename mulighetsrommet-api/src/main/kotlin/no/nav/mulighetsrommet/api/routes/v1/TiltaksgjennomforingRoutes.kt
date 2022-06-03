package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import org.koin.ktor.ext.inject

fun Route.tiltaksgjennomforingRoutes() {

    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get() {
            call.respond(tiltaksgjennomforingService.getTiltaksgjennomforinger())
        }
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing = tiltaksgjennomforingService.getTiltaksgjennomforingById(id) ?: return@get call.respondText(
                "Det finner ikke noe tiltak med id $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(tiltaksgjennomforing)
        }
        post() {
            runCatching {
                val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
                tiltaksgjennomforingService.createTiltaksgjennomforing(tiltaksgjennomforing)
            }.onSuccess { createdTiltakstype ->
                call.response.status(HttpStatusCode.Created)
                call.respond(createdTiltakstype)
            }.onFailure {
                call.respondText("Kunne ikke opprette tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }
        put("{id}") {
            runCatching {
                val arenaId = call.parameters["id"]!!.toInt()
                val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
                tiltaksgjennomforingService.updateTiltaksgjennomforing(arenaId, tiltaksgjennomforing)
            }.onSuccess { updatedTiltakstype ->
                call.respond(updatedTiltakstype)
            }.onFailure {
                call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
