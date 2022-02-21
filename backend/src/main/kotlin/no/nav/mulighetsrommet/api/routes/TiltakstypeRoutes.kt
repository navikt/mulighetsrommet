package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.koin.ktor.ext.inject

fun Parameters.parseList(parameter: String): List<String> {
    return entries().filter { it.key == parameter }.flatMap { it.value }
}

fun Route.tiltakstypeRoutes() {

    val tiltakstypeService: TiltakstypeService by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    get("/api/tiltakstyper") {
        val search = call.request.queryParameters["search"]

        val innsatsgrupper = call.request.queryParameters.parseList("innsatsgrupper").map { Integer.parseInt(it) }

        val items = tiltakstypeService.getTiltakstyper(innsatsgrupper, search)
        call.respond(items)
    }
    get("/api/tiltakstyper/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
            "Mangler eller ugyldig",
            status = HttpStatusCode.BadRequest
        )
        val tiltak = tiltakstypeService.getTiltakstypeById(id) ?: return@get call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        call.respond(tiltak)
    }
    get("/api/tiltakstyper/{id}/tiltaksgjennomforinger") {

        val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )
        val tiltaksgjennomforinger = tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakstypeId(id)
        call.respond(tiltaksgjennomforinger)
    }
    post("/api/tiltakstyper") {
        val tiltakstype = call.receive<Tiltakstype>()
        val createdTiltak = tiltakstypeService.createTiltakstype(tiltakstype)
        call.respond(HttpStatusCode.Created, createdTiltak)
    }
    put("/api/tiltakstyper/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )
        tiltakstypeService.getTiltakstypeById(id) ?: return@put call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        val tiltakstype = call.receive<Tiltakstype>()
        val updatedTiltakstype = tiltakstypeService.updateTiltakstype(id, tiltakstype)
        call.respond(updatedTiltakstype!!)
    }
    delete("/api/tiltakstyper/{id}") {
        val id = call.parameters["id"] ?: return@delete call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest
        )

        val tiltakstype = tiltakstypeService.getTiltakstypeById(id.toInt()) ?: return@delete call.respondText(
            "Det finner ikke noe tiltak med id $id",
            status = HttpStatusCode.NotFound
        )
        call.respond(tiltakstypeService.archivedTiltakstype(tiltakstype))
    }
}
