package no.nav.amt_informasjon_api.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.amt_informasjon_api.domain.Tiltaksvariant
import no.nav.amt_informasjon_api.services.TiltaksgjennomforingService
import no.nav.amt_informasjon_api.services.TiltaksvariantService

fun Route.tiltaksvariantRoutes(
    tiltaksvariantService: TiltaksvariantService,
    tiltaksgjennomforingService: TiltaksgjennomforingService
) {
    get("/api/tiltaksvarianter") {
        val innsatsgruppe = call.request.queryParameters["innsatsgruppe"]?.toIntOrNull()
        val items = tiltaksvariantService.getTiltaksvarianter(innsatsgruppe)
        call.respond(items)
    }
    get("/api/tiltaksvarianter/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
            "Mangler eller ugyldig",
            status = HttpStatusCode.BadRequest
        )
        val tiltak = tiltaksvariantService.getTiltaksvariantById(id) ?: return@get call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        call.respond(tiltak)
    }
    get("/api/tiltaksvarianter/{id}/tiltaksgjennomforinger") {

        val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )
        val tiltaksgjennomforinger = tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltaksvariantId(id)
        call.respond(tiltaksgjennomforinger)
    }
    post("/api/tiltaksvarianter") {
        val tiltaksvariant = call.receive<Tiltaksvariant>()
        val createdTiltak = tiltaksvariantService.createTiltaksvariant(tiltaksvariant)
        call.respond(HttpStatusCode.Created, createdTiltak)
    }
    put("/api/tiltaksvarianter/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )
        tiltaksvariantService.getTiltaksvariantById(id) ?: return@put call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        val tiltaksvariant = call.receive<Tiltaksvariant>()
        val updatedTiltaksvariant = tiltaksvariantService.updateTiltaksvariant(id, tiltaksvariant)
        call.respond(updatedTiltaksvariant!!)
    }
    delete("/api/tiltaksvarianter/{id}") {
        val id = call.parameters["id"] ?: return@delete call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )

        val tiltaksvariant = tiltaksvariantService.getTiltaksvariantById(id.toInt()) ?: return@delete call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        call.respond(tiltaksvariantService.archivedTiltaksvariant(tiltaksvariant))
    }
}
