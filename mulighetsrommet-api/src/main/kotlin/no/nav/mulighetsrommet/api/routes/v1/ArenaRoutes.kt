package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.util.pipeline.*
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
        put("tiltakstype") {
            runCatching {
                val tiltakstype = call.receive<AdapterTiltak>()
                arenaService.upsertTiltakstype(tiltakstype)
            }.onSuccess { updatedTiltakstype ->
                call.respond(updatedTiltakstype)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("tiltakstype") {
            runCatching {
                val tiltakstype = call.receive<AdapterTiltak>()
                arenaService.deleteTiltakstype(tiltakstype)
            }.onSuccess {
                call.response.status(HttpStatusCode.OK)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke slette tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }

        put("tiltaksgjennomforing") {
            runCatching {
                val tiltaksgjennomforing = call.receive<AdapterTiltaksgjennomforing>()
                arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            }.onSuccess { createdTiltaksgjennomforing ->
                call.respond(createdTiltaksgjennomforing)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke opprette tiltak", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("tiltaksgjennomforing") {
            runCatching {
                val tiltaksgjennomforing = call.receive<AdapterTiltaksgjennomforing>()
                arenaService.deleteTiltaksgjennomforing(tiltaksgjennomforing)
            }.onSuccess {
                call.response.status(HttpStatusCode.OK)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke slette tiltak", status = HttpStatusCode.InternalServerError)
            }
        }

        put("deltaker") {
            runCatching {
                val deltaker = call.receive<AdapterTiltakdeltaker>()
                arenaService.upsertDeltaker(deltaker)
            }.onSuccess { createdDeltaker ->
                call.respond(createdDeltaker)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke opprette deltaker", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("deltaker") {
            runCatching {
                val deltaker = call.receive<AdapterTiltakdeltaker>()
                arenaService.deleteDeltaker(deltaker)
            }.onSuccess {
                call.response.status(HttpStatusCode.OK)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke slette deltaker", status = HttpStatusCode.InternalServerError)
            }
        }

        put("sak") {
            runCatching {
                val sak = call.receive<AdapterSak>()
                arenaService.updateTiltaksgjennomforingWithSak(sak)
            }.onSuccess {
                val response = it ?: HttpStatusCode.Conflict
                call.respond(response)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke oppdatere tiltak med sak", status = HttpStatusCode.InternalServerError)
            }
        }

        delete("sak") {
            runCatching {
                val sak = call.receive<AdapterSak>()
                arenaService.unsetSakOnTiltaksgjennomforing(sak)
            }.onSuccess {
                call.response.status(HttpStatusCode.OK)
            }.onFailure {
                logError(logger, it)
                call.respondText("Kunne ikke oppdatere tiltak med sak", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.logError(logger: Logger, error: Throwable) {
    logger.debug(
        "Error during at request handler method=${this.context.request.httpMethod.value} path=${this.context.request.path()}",
        error
    )
}
