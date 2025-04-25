package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingArenaDataDto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.externalRoutes() {
    val gjennomforingService: GjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get {
            val orgnr = call.request.queryParameters.getOrFail<Organisasjonsnummer>("orgnr")

            val filter = EksternTiltaksgjennomforingFilter(arrangorOrgnr = listOf(orgnr))
            val pagination = getPaginationParams()

            val result = gjennomforingService.getAllEkstern(pagination, filter)

            call.respond(result)
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = gjennomforingService.getEkstern(id)
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

data class EksternTiltaksgjennomforingFilter(
    val arrangorOrgnr: List<Organisasjonsnummer> = emptyList(),
)

fun toArenaDataDto(tiltaksnummer: String) = TiltaksgjennomforingArenaDataDto(
    opprettetAar = tiltaksnummer.split("#").first().toInt(),
    lopenr = tiltaksnummer.split("#")[1].toInt(),
)
