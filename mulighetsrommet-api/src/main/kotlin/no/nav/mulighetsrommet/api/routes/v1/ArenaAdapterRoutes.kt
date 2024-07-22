package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.logging.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.services.ArenaAdapterService
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.UpsertTiltaksgjennomforingResponse
import org.koin.ktor.ext.inject
import org.postgresql.util.PSQLException
import java.util.*

fun Route.arenaAdapterRoutes() {
    val logger = application.environment.log

    val arenaAdapterService: ArenaAdapterService by inject()

    route("/api/v1/intern/arena/") {
        put("avtale") {
            val dbo = call.receive<ArenaAvtaleDbo>()

            call.respond(arenaAdapterService.upsertAvtale(dbo))
        }

        delete("avtale/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            arenaAdapterService.removeAvtale(id)
            call.response.status(HttpStatusCode.OK)
        }

        put("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<ArenaTiltaksgjennomforingDbo>()

            val sanityId = arenaAdapterService.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            call.respond(UpsertTiltaksgjennomforingResponse(sanityId))
        }

        delete("tiltaksgjennomforing/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            arenaAdapterService.removeTiltaksgjennomforing(id)
            call.response.status(HttpStatusCode.OK)
        }

        delete("sanity/tiltaksgjennomforing/{sanityId}") {
            val sanityId = call.parameters.getOrFail<UUID>("sanityId")

            arenaAdapterService.removeSanityTiltaksgjennomforing(sanityId)
            call.response.status(HttpStatusCode.OK)
        }

        put("tiltakshistorikk") {
            val tiltakshistorikk = call.receive<ArenaTiltakshistorikkDbo>()

            arenaAdapterService.upsertTiltakshistorikk(tiltakshistorikk)
                .map { call.respond(HttpStatusCode.OK, it) }
                .mapLeft {
                    logger.warn("Error during upsertTiltakshistorikk: $it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette tiltakshistorikk")
                }
        }

        delete("tiltakshistorikk/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            arenaAdapterService.removeTiltakshistorikk(id)
                .map { call.response.status(HttpStatusCode.OK) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette tiltakshistorikk")
                }
        }

        put("deltaker") {
            val deltaker = call.receive<DeltakerDbo>()

            arenaAdapterService.upsertDeltaker(deltaker)
                .onRight { call.respond(it) }
                .onLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette deltaker")
                }
        }

        delete("deltaker/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            arenaAdapterService.removeDeltaker(id)
                .onRight { call.response.status(HttpStatusCode.OK) }
                .onLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke slette deltaker")
                }
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.logError(logger: Logger, error: PSQLException) {
    logger.warn(
        "Error during at request handler method=${this.context.request.httpMethod.value} path=${this.context.request.path()}",
        error,
    )
}
