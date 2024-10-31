package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.services.ArenaAdapterService
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.UpsertTiltaksgjennomforingResponse
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arenaAdapterRoutes() {
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
    }
}
