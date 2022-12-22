package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.apiRoutes() {
    val arenaEntityService: ArenaEntityService by inject()

    get("api/exchange/{tiltaksnummer}") {
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
        call.respond(ExchangeTiltaksnummerForIdResponse(uuid))
    }
}

@Serializable
data class ExchangeTiltaksnummerForIdResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
)
