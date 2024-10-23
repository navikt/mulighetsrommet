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
import no.nav.mulighetsrommet.domain.dto.UpsertTiltaksgjennomforingResponse
import org.koin.ktor.ext.inject
import org.postgresql.util.PSQLException
import java.util.*

fun Route.arenaAdapterRoutes() {
    val arenaAdapterService: ArenaAdapterService by inject()

    route("/api/v1/intern/arena/") {
        put("avtale") {
            val dbo = call.receive<ArenaAvtaleDbo>()

            call.respond(arenaAdapterService.upsertAvtale(dbo))
        }

        put("tiltaksgjennomforing") {
            val tiltaksgjennomforing = call.receive<ArenaTiltaksgjennomforingDbo>()

            val sanityId = arenaAdapterService.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            call.respond(UpsertTiltaksgjennomforingResponse(sanityId))
        }

        delete("sanity/tiltaksgjennomforing/{sanityId}") {
            val sanityId = call.parameters.getOrFail<UUID>("sanityId")

            arenaAdapterService.removeSanityTiltaksgjennomforing(sanityId)
            call.response.status(HttpStatusCode.OK)
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.logError(logger: Logger, error: PSQLException) {
    logger.warn(
        "Error during at request handler method=${this.context.request.httpMethod.value} path=${this.context.request.path()}",
        error,
    )
}
