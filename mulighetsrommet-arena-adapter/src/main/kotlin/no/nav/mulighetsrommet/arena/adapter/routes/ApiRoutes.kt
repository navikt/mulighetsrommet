package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.utils.toUUID
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsstatusDto
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val arenaEntityService: ArenaEntityService by inject()

    get("/api/exchange/{arenaId}") {
        val arenaId = call.parameters["arenaId"] ?: return@get call.respondText(
            "Mangler eller ugyldig tiltaksnummer",
            status = HttpStatusCode.BadRequest
        )

        val uuid =
            arenaEntityService.getMappingIfProcessed(ArenaTables.Tiltaksgjennomforing, arenaId)?.entityId
                ?: return@get call.respondText(
                    "Det finnes ikke noe prossesert tiltaksgjennomføring med arena-id $arenaId",
                    status = HttpStatusCode.NotFound
                )
        call.respond(ExchangeArenaIdForIdResponse(uuid))
    }

    get("/api/status/{id}") {
        val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
            "Mangler eller ugyldig tiltaksnummer",
            status = HttpStatusCode.BadRequest
        )

        val tiltaksgjennomforing = arenaEntityService.getTiltaksgjennomforing(id)
            ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound
            )

        call.respond(TiltaksgjennomforingsstatusDto(tiltaksgjennomforing.status))
    }
}
