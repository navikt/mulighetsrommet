package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val arenaEntityService: ArenaEntityService by inject()

    get("/api/exchange/{tiltaksnummer}") {
        val tiltaksnummer = call.parameters["tiltaksnummer"] ?: return@get call.respondText(
            "Mangler eller ugyldig tiltaksnummer",
            status = HttpStatusCode.BadRequest
        )

        val uuid =
            arenaEntityService.getMappingIfProcessed(ArenaTables.Tiltaksgjennomforing, tiltaksnummer)?.entityId
                ?: return@get call.respondText(
                    "Det finnes ikke noe prossesert tiltaksgjennomføring med tiltaksnummer $tiltaksnummer",
                    status = HttpStatusCode.NotFound
                )
        call.respond(ExchangeArenaIdForIdResponse(uuid))
    }
}
