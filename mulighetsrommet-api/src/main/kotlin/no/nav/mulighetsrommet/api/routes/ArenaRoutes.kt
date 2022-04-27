package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.mulighetsrommet.api.services.ArenaService
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import org.koin.ktor.ext.inject

fun Route.arenaRoutes() {

    val arenaService: ArenaService by inject()

    post("/api/arena/tiltakstyper") {
        runCatching {
            val tiltakstype = call.receive<Tiltakstype>()
            arenaService.createTiltakstype(tiltakstype)
        }.onSuccess { createdTiltakstype ->
            call.response.status(HttpStatusCode.Created)
            call.respond(createdTiltakstype)
        }.onFailure {
            call.respondText("Kunne ikke opprette tiltakstype", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/arena/tiltakstyper/{tiltakskode}") {
        runCatching {
            val tiltakskode = Tiltakskode.valueOf(call.parameters["tiltakskode"]!!)
            val tiltakstype = call.receive<Tiltakstype>()
            arenaService.updateTiltakstype(tiltakskode, tiltakstype)
        }.onSuccess { updatedTiltakstype ->
            call.respond(updatedTiltakstype)
        }.onFailure {
            call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
        }
    }
    post("/api/arena/tiltaksgjennomforinger") {
        runCatching {
            val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
            arenaService.createTiltaksgjennomforing(tiltaksgjennomforing)
        }.onSuccess { createdTiltaksgjennomforing ->
            call.response.status(HttpStatusCode.Created)
            call.respond(createdTiltaksgjennomforing)
        }.onFailure {
            call.respondText("Kunne ikke opprette tiltaksgjennomføring", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/arena/tiltaksgjennomforinger/{arenaId}") {
        runCatching {
            val arenaId = call.parameters["arenaId"]!!.toInt()
            val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
            arenaService.updateTiltaksgjennomforing(arenaId, tiltaksgjennomforing)
        }.onSuccess { updatedTiltaksgjennomforing ->
            call.respond(updatedTiltaksgjennomforing)
        }.onFailure {
            call.respondText("Kunne ikke oppdatere tiltaksgjennomføring", status = HttpStatusCode.InternalServerError)
        }
    }

}
