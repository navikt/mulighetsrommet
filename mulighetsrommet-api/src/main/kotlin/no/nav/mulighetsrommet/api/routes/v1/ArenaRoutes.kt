package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.ArenaService
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.koin.ktor.ext.inject

fun Route.arenaRoutes() {

    val arenaService: ArenaService by inject()

    route("/api/v1/arena/") {
        put("tiltakstyper") {
            runCatching {
                val tiltakstype = call.receive<Tiltakstype>()
                arenaService.upsertTiltakstype(tiltakstype)
            }.onSuccess { updatedTiltakstype ->
                call.respond(updatedTiltakstype)
            }.onFailure {
                application.environment.log.debug("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }

        put("tiltaksgjennomforinger") {
            runCatching {
                val tiltaksgjennomforing = call.receive<Tiltaksgjennomforing>()
                arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            }.onSuccess { createdTiltaksgjennomforing ->
                call.response.status(HttpStatusCode.Created)
                call.respond(createdTiltaksgjennomforing)
            }.onFailure {
                application.environment.log.debug("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText(
                    "Kunne ikke opprette tiltaksgjennomføring",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        put("deltakere") {
            runCatching {
                val deltaker = call.receive<Deltaker>()
                arenaService.upsertDeltaker(deltaker)
            }.onSuccess { createdDeltaker ->
                call.response.status(HttpStatusCode.Created)
                call.respond(createdDeltaker)
            }.onFailure {
                application.environment.log.debug("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke opprette deltaker", status = HttpStatusCode.InternalServerError)
            }
        }

        put("sak") {
            runCatching {
                val sak = call.receive<ArenaSak>()
                arenaService.updateTiltaksgjennomforingWithSak(sak)
            }.onSuccess {
                val response = it ?: HttpStatusCode.NotFound
                call.respond(response)
            }.onFailure {
                application.environment.log.debug("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText(
                    "Kunne ikke oppdatere tiltaksgjennomføring med sak: ${it.stackTraceToString()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}
