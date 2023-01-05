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
import no.nav.mulighetsrommet.domain.dbo.HistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
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

        delete("tiltakstype") {
            val tiltakstype = call.receive<TiltakstypeDbo>()
            arenaService.remove(tiltakstype)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltakstype")
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

        delete("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<TiltaksgjennomforingDbo>()
            arenaService.remove(tiltaksgjennomforing)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltak")
                }
        }

        put("deltaker") {
            /*call.respond<HistorikkDbo>(
                HistorikkDbo.Gruppetiltak(
                    id = "35ea42d5-b0cf-4de3-b249-8dbc7c02c7bb".toUUID(),
                    norskIdent = "1234354",
                    status = Deltakerstatus.DELTAR,
                    fraDato = null,
                    tilDato = null,
                    tiltaksgjennomforingId = "35ea42d5-b0cf-4de3-b249-8dbc7c02c7bb".toUUID()
                )
            )*/
            val deltaker = call.receive<HistorikkDbo>()
            arenaService.upsert(deltaker)
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
            val deltaker = call.receive<HistorikkDbo>()
            arenaService.remove(deltaker)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
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
