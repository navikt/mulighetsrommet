package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
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

fun Route.tiltakstypeRoutes() {

    val tiltakstypeService: TiltakstypeService by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    // get("/api/tiltakstyper") {
    //     val innsatsgruppe = call.request.queryParameters["innsatsgruppe"]?.toIntOrNull()
    //     // val items = tiltakstypeService.getTiltakstyper(innsatsgruppe)
    //     call.respond(items)
    // }
    // get("/api/tiltakstyper/{id}") {
    //     val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
    //         "Mangler eller ugyldig",
    //         status = HttpStatusCode.BadRequest
    //     )
    //     val tiltak = tiltakstypeService.getTiltakstypeById(id) ?: return@get call.respondText(
    //         "Det finner ikke noe tiltak med id $id",
    //         status = HttpStatusCode.NotFound
    //     )
    //     call.respond(tiltak)
    // }
    // get("/api/tiltakstyper/{id}/tiltaksgjennomforinger") {
    //
    //     val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
    //         "Mangler eller ugyldig id",
    //         status = HttpStatusCode.BadRequest
    //     )
    //     val tiltaksgjennomforinger = tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakstypeId(id)
    //     call.respond(tiltaksgjennomforinger)
    // }
}
