package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.ArenaService
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import org.koin.ktor.ext.inject

fun Route.arenaRoutes() {

    val logger = application.environment.log

    val arenaService: ArenaService by inject()

    route("/api/v1/arena/") {
        put("tiltakstyper") {
            runCatching {
                val tiltakstype = call.receive<AdapterTiltak>()
                arenaService.upsertTiltakstype(tiltakstype)
            }.onSuccess { updatedTiltakstype ->
                call.respond(updatedTiltakstype)
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }

        put("tiltaksgjennomforinger") {
            runCatching {
                val tiltaksgjennomforing = call.receive<AdapterTiltaksgjennomforing>()
                arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            }.onSuccess { createdTiltaksgjennomforing ->
                call.respond(createdTiltaksgjennomforing)
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke opprette tiltak", status = HttpStatusCode.InternalServerError)
            }
        }

        put("deltakere") {
            runCatching {
                val deltaker = call.receive<AdapterTiltakdeltaker>()
                arenaService.upsertDeltaker(deltaker)
            }.onSuccess { createdDeltaker ->
                call.respond(createdDeltaker)
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke opprette deltaker", status = HttpStatusCode.InternalServerError)
            }
        }

        put("sak") {
            runCatching {
                val sak = call.receive<AdapterSak>()
                arenaService.updateTiltaksgjennomforingWithSak(sak)
            }.onSuccess {
                val response = it ?: HttpStatusCode.NotFound
                call.respond(response)
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke oppdatere tiltak med sak", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
