package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.domain.dto.ArenaTiltaksgjennomforingsstatusDto
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val arenaEntityService: ArenaEntityService by inject()

    get("/api/exchange/{arenaId}") {
        val arenaId = call.parameters["arenaId"] ?: return@get call.respondText(
            "Mangler eller ugyldig arena-id",
            status = HttpStatusCode.BadRequest,
        )

        val mapping = arenaEntityService.getMappingIfHandled(ArenaTable.Tiltaksgjennomforing, arenaId)
            ?: return@get call.respondText(
                "Det finnes ikke noe prosessert tiltaksgjennomføring med arena-id $arenaId",
                status = HttpStatusCode.NotFound,
            )

        call.respond(ExchangeArenaIdForIdResponse(mapping.entityId))
    }

    get("/api/status/{id}") {
        val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
            "Mangler eller ugyldig id",
            status = HttpStatusCode.BadRequest,
        )

        val tiltaksgjennomforing = arenaEntityService.getTiltaksgjennomforingOrNull(id)
            ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound,
            )

        call.respond(ArenaTiltaksgjennomforingsstatusDto(tiltaksgjennomforing.status))
    }
}
