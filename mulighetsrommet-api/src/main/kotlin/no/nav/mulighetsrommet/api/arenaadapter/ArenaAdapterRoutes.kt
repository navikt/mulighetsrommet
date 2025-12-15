package no.nav.mulighetsrommet.api.arenaadapter

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.UpsertTiltaksgjennomforingResponse
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.arenaAdapterRoutes() {
    val arenaAdapterService: ArenaAdapterService by inject()

    route("/api/v1/intern/arena/") {
        put("avtale") {
            val dbo = call.receive<ArenaAvtaleDbo>()

            call.respond(arenaAdapterService.upsertAvtale(dbo))
        }

        put("tiltaksgjennomforing") {
            val gjennomforing = call.receive<ArenaGjennomforingDbo>()

            val sanityId = arenaAdapterService.upsertTiltaksgjennomforing(gjennomforing)

            call.respond(UpsertTiltaksgjennomforingResponse(sanityId))
        }

        delete("sanity/tiltaksgjennomforing/{sanityId}") {
            val sanityId = call.parameters.getOrFail<UUID>("sanityId")

            arenaAdapterService.removeSanityTiltaksgjennomforing(sanityId)
            call.response.status(HttpStatusCode.OK)
        }
    }
}
