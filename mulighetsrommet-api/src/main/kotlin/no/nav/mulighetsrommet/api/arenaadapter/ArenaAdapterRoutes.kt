package no.nav.mulighetsrommet.api.arenaadapter

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
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
