package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsArenadataDto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.externalRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1/tiltaksgjennomforinger") {
        get {
            val orgnr = call.request.queryParameters.getOrFail("orgnr")
            val filter = AdminTiltaksgjennomforingFilter(arrangorOrgnr = orgnr)
            val paginationParams = getPaginationParams()

            val result = tiltaksgjennomforingService.getAll(paginationParams, filter)
                .let {
                    val data = it.data.map { dto -> TiltaksgjennomforingDto.from(dto) }
                    PaginatedResponse(pagination = it.pagination, data = data)
                }

            call.respond(result)
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = tiltaksgjennomforingService.get(id)
                ?.let { TiltaksgjennomforingDto.from(it) }
                ?: return@get call.respond(HttpStatusCode.Companion.NotFound, "Ingen tiltaksgjennomføring med id=$id")

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

            val gjennomforing = tiltaksgjennomforingService.get(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            if (gjennomforing.opphav == ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                return@get call.respond(HttpStatusCode.OK)
            }

            val arenaData = arenaAdapterService.hentArenadata(id)
                ?.let { TiltaksgjennomforingsArenadataDto.from(gjennomforing, it.status) }
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, arenaData)
        }
    }
}
