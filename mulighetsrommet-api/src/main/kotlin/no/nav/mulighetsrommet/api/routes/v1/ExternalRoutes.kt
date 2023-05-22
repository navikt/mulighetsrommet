package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsArenadataDto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.externalRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1") {
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            tiltaksgjennomforingService.get(id)
                .onRight {
                    if (it == null) {
                        return@get call.respondText(
                            "Det finnes ikke noe tiltaksgjennomføring med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    call.respond(TiltaksgjennomforingDto.from(it))
                }
                .onLeft { error ->
                    log.error("$error")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente gjennomføring")
                }
        }

        get("tiltaksgjennomforinger/id/{arenaId}") {
            val arenaId = call.parameters.getOrFail("arenaId")
            val idResponse = arenaAdapterService.exchangeTiltaksgjennomforingsArenaIdForId(arenaId)
                ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med arenaId $arenaId",
                    status = HttpStatusCode.NotFound,
                )
            call.respond(idResponse)
        }

        get("tiltaksgjennomforinger/arenadata/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            tiltaksgjennomforingService.get(id)
                .map {
                    if (it == null) {
                        return@get call.respondText(
                            "Det finnes ikke noe tiltaksgjennomføring med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    val status = arenaAdapterService.hentTiltaksgjennomforingsstatus(id)?.status ?: return@get call.respondText(
                        "Det finnes ikke noe tiltaksgjennomføring med id $id",
                        status = HttpStatusCode.NotFound,
                    )
                    call.respond(TiltaksgjennomforingsArenadataDto.from(it, status))
                }
                .onLeft { error ->
                    log.error("$error")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente gjennomføring")
                }
        }
    }
}
