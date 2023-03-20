package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsArenadataDto
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject

fun Route.externalRoutes() {
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1") {
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing = tiltaksgjennomforinger.get(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(TiltaksgjennomforingDto.from(tiltaksgjennomforing))
        }

        get("tiltaksgjennomforinger/id/{arenaId}") {
            val arenaId = call.parameters["arenaId"] ?: return@get call.respondText(
                "Mangler eller ugyldig arenaId",
                status = HttpStatusCode.BadRequest
            )
            val idResponse = arenaAdapterService.exchangeTiltaksgjennomforingsArenaIdForId(arenaId)
                ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med arenaId $arenaId",
                    status = HttpStatusCode.NotFound
                )
            call.respond(idResponse)
        }

        get("tiltaksgjennomforinger/arenadata/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing = tiltaksgjennomforinger.get(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound
            )
            val status = arenaAdapterService.hentTiltaksgjennomforingsstatus(id)?.status ?: return@get call.respondText(
                "Det finnes ikke noe tiltaksgjennomføring med id $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(TiltaksgjennomforingsArenadataDto.from(tiltaksgjennomforing, status))
        }
    }
}
