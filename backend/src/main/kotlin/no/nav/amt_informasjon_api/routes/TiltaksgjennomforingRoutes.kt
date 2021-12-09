package no.nav.amt_informasjon_api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.amt_informasjon_api.services.TiltaksgjennomforingService

fun Route.tiltaksgjennomforingRoutes(service: TiltaksgjennomforingService) {
    // TODO: Vurder om det blir mer REST-ish å ha disse på /api/tiltakstyper/tiltaksgjennomforinger/
    get("/api/tiltaksgjennomforinger") {
        call.respond(service.getTiltaksgjennomforinger())
    }
    get("/api/tiltaksgjennomforinger/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )
        val tiltaksgjennomforing = service.getTiltaksgjennomforingById(id) ?: return@get call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        call.respond(tiltaksgjennomforing)
    }
}
