package no.nav.mulighetsrommet.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.ArenaService
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.domain.arena.ArenaSak
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
    post("/api/arena/deltakere") {
        runCatching {
            val deltaker = call.receive<Deltaker>()
            arenaService.createDeltaker(deltaker)
        }.onSuccess { createdDeltaker ->
            call.response.status(HttpStatusCode.Created)
            call.respond(createdDeltaker)
        }.onFailure {
            call.respondText("Kunne ikke opprette deltaker", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/arena/deltakere/{arenaId}") {
        runCatching {
            val arenaId = call.parameters["arenaId"]!!.toInt()
            val deltaker = call.receive<Deltaker>()
            arenaService.updateDeltaker(arenaId, deltaker)
        }.onSuccess { updatedDeltaker ->
            call.respond(updatedDeltaker)
        }.onFailure {
            call.respondText("Kunne ikke oppdatere deltaker", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/arena/sak/{sakId}") {
        runCatching {
            val sakId = call.parameters["sakId"]!!.toInt()
            val sak = call.receive<ArenaSak>()
            arenaService.updateTiltaksgjennomforingWithSak(sakId, sak)
        }.onSuccess { call.respond(it) }.onFailure {
            call.application.environment.log.debug(it.stackTraceToString())
            call.respondText(
                "FEIL: ${it.stackTraceToString()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
