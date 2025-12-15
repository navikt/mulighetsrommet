package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.model.ArenaTiltaksgjennomforingDto
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.apiRoutes() {
    val arenaEntityService: ArenaEntityService by inject()

    get("/api/exchange/{arenaId}") {
        val arenaId = call.parameters.getOrFail("arenaId")

        val mapping = arenaEntityService.getMappingIfHandled(ArenaTable.Tiltaksgjennomforing, arenaId)
            ?: return@get call.respondText(
                "Det finnes ikke noe prosessert tiltaksgjennomføring med arena-id $arenaId",
                status = HttpStatusCode.NotFound,
            )

        call.respond(no.nav.mulighetsrommet.model.ExchangeArenaIdForIdResponse(mapping.entityId))
    }

    get("/api/arenadata/{id}") {
        val id = call.parameters.getOrFail<UUID>("id")

        val tiltaksgjennomforing = arenaEntityService.getTiltaksgjennomforingOrNull(id)
            ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound,
            )

        call.respond(ArenaTiltaksgjennomforingDto(tiltaksgjennomforing.tiltaksgjennomforingId, tiltaksgjennomforing.status))
    }
}
