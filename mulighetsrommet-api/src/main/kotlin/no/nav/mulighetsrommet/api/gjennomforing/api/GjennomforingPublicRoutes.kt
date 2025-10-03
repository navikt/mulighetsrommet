package no.nav.mulighetsrommet.api.gjennomforing.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.model.TiltaksgjennomforingArenaDataDto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.gjennomforingPublicRoutes() {
    val gjennomforingService: GjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get("{id}") {
            val id: UUID by call.parameters

            val result = gjennomforingService.get(id)
                ?.let { TiltaksgjennomforingV1Mapper.fromGjennomforing(it) }
                ?: return@get call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")

            call.respond(result)
        }

        get("id/{arenaId}") {
            val arenaId = call.parameters.getOrFail("arenaId")
            val idResponse = arenaAdapterService.exchangeTiltaksgjennomforingsArenaIdForId(arenaId)
                ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med arenaId $arenaId",
                    status = HttpStatusCode.NotFound,
                )
            call.respond(idResponse)
        }

        get("arenadata/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val gjennomforing = gjennomforingService.get(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            val arenaData = gjennomforing.tiltaksnummer?.let { toArenaDataDto(it) }
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(HttpStatusCode.OK, arenaData)
        }
    }
}

fun toArenaDataDto(tiltaksnummer: String) = TiltaksgjennomforingArenaDataDto(
    opprettetAar = tiltaksnummer.split("#").first().toInt(),
    lopenr = tiltaksnummer.split("#")[1].toInt(),
)
