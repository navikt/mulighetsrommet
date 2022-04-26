package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import org.koin.ktor.ext.inject

fun Route.tiltaksgjennomforingRoutes() {

    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    get("/api/tiltaksgjennomforinger") {
        call.respond(tiltaksgjennomforingService.getTiltaksgjennomforinger())
    }
    get("/api/tiltaksgjennomforinger/{id}") {
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
    post("/api/tiltaksgjennomforinger") {
        runCatching {
            val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
            tiltaksgjennomforingService.createTiltaksgjennomforing(tiltaksgjennomforing)
        }.onSuccess { createdTiltakstype ->
            call.response.status(HttpStatusCode.Created)
            call.respond(createdTiltakstype)
        }.onFailure {
            it.printStackTrace()
            call.respondText("Kunne ikke opprette tiltakstype", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/tiltaksgjennomforinger/{id}") {
        runCatching {
            val arenaId = call.parameters["id"]!!.toInt()
            val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
            tiltaksgjennomforingService.updateTiltaksgjennomforing(arenaId, tiltaksgjennomforing)
        }.onSuccess { updatedTiltakstype ->
            call.respond(updatedTiltakstype)
        }
    }
}
