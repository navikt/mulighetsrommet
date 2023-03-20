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
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject
import org.postgresql.util.PSQLException

fun Route.arenaRoutes() {
    val logger = application.environment.log

    val arenaService: ArenaService by inject()

    route("/api/v1/internal/arena/") {
        put("tiltakstype") {
            val tiltakstype = call.receive<TiltakstypeDbo>()

            arenaService.upsert(tiltakstype)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke oppdatere tiltakstype")
                }
        }

        delete("tiltakstype/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@delete call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )

            arenaService.removeTiltakstype(id)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltakstype")
                }
        }

        put("avtale") {
            val dbo = call.receive<AvtaleDbo>()

            arenaService.upsert(dbo)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette avtale")
                }
        }

        delete("avtale/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@delete call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )

            arenaService.removeAvtale(id)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette avtale")
                }
        }

        put("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<TiltaksgjennomforingDbo>()

            arenaService.upsert(tiltaksgjennomforing)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette tiltak")
                }
        }

        delete("tiltaksgjennomforing/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@delete call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )

            arenaService.removeTiltaksgjennomforing(id)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltak")
                }
        }

        put("tiltakshistorikk") {
            val tiltakshistorikk = call.receive<TiltakshistorikkDbo>()

            arenaService.upsert(tiltakshistorikk)
                .map { call.respond(HttpStatusCode.OK, it) }
                .mapLeft {
                    when (it) {
                        is DatabaseOperationError.ForeignKeyViolation -> {
                            call.respond(HttpStatusCode.Conflict, "Kunne ikke opprette tiltakshistorikk")
                        }

                        else -> {
                            logError(logger, it.error)
                            call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette tiltakshistorikk")
                        }
                    }
                }
        }

        delete("tiltakshistorikk/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@delete call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            arenaService.removeTiltakshistorikk(id)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltakshistorikk")
                }
        }

        put("deltaker") {
            val deltaker = call.receive<DeltakerDbo>()

            arenaService.upsertDeltaker(deltaker)
                .onRight { call.respond(it) }
                .onLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette deltaker")
                }
        }

        delete("deltaker/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@delete call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )

            arenaService.removeDeltaker(id)
                .onRight { call.response.status(HttpStatusCode.OK) }
                .onLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette deltaker")
                }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.logError(logger: Logger, error: PSQLException) {
    logger.debug(
        "Error during at request handler method=${this.context.request.httpMethod.value} path=${this.context.request.path()}",
        error
    )
}
