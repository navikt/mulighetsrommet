package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.services.ArenaService
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import org.koin.ktor.ext.inject
import org.postgresql.util.PSQLException

fun Route.arenaRoutes() {

    val logger = application.environment.log

    val arenaService: ArenaService by inject()

    route("/api/v1/arena/") {
        put("tiltakstype") {
            val tiltakstype = call.receive<AdapterTiltak>()
            arenaService.upsertTiltakstype(tiltakstype)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke oppdatere tiltakstype")
                }
        }

        delete("tiltakstype") {
            val tiltakstype = call.receive<AdapterTiltak>()
            arenaService.deleteTiltakstype(tiltakstype)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltakstype")
                }
        }

        put("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<AdapterTiltaksgjennomforing>()
            arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette tiltak")
                }
        }

        delete("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<AdapterTiltaksgjennomforing>()
            arenaService.deleteTiltaksgjennomforing(tiltaksgjennomforing)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltak")
                }
        }

        put("deltaker") {
            val deltaker = call.receive<AdapterTiltakdeltaker>()
            arenaService.upsertDeltaker(deltaker)
                .map { call.respond(HttpStatusCode.OK, it) }
                .mapLeft {
                    when (it) {
                        is DatabaseOperationError.ForeignKeyViolation -> {
                            call.respond(HttpStatusCode.Conflict, "Kunne ikke opprette deltaker")
                        }
                        else -> {
                            logError(logger, it.error)
                            call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette deltaker")
                        }
                    }
                }
        }

        delete("deltaker") {
            val deltaker = call.receive<AdapterTiltakdeltaker>()
            arenaService.deleteDeltaker(deltaker)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette deltaker")
                }
        }
    }

    put("sak") {
        val sak = call.receive<AdapterSak>()
        arenaService.updateTiltaksgjennomforingWithSak(sak)
            .map {
                val response = it ?: HttpStatusCode.Conflict
                call.respond(response)
            }
            .mapLeft {
                logError(logger, it.error)
                call.respond(HttpStatusCode.InternalServerError, "Kunne ikke oppdatere tiltak med sak")
            }
    }

    delete("sak") {
        val sak = call.receive<AdapterSak>()
        arenaService.unsetSakOnTiltaksgjennomforing(sak)
            .map { call.response.status(HttpStatusCode.OK) }
            .mapLeft {
                logError(logger, it.error)
                call.respond(HttpStatusCode.InternalServerError, "Kunne ikke oppdatere tiltak med sak")
            }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.logError(logger: Logger, error: PSQLException) {
    logger.debug(
        "Error during at request handler method=${this.context.request.httpMethod.value} path=${this.context.request.path()}",
        error
    )
}
