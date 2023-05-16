package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsArenadataDto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.externalRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/api/v1") {
        get("tiltaksgjennomforinger") {
            val orgnr = call.request.queryParameters.getOrFail("orgnr")
            val paginationParams = getPaginationParams()
            val filter = AdminTiltaksgjennomforingFilter(organisasjonsnummer = orgnr)

            val result = tiltaksgjennomforingService.getAll(paginationParams, filter)
                .map {
                    val data = it.data.map { dto -> TiltaksgjennomforingDto.from(dto) }
                    PaginatedResponse(pagination = it.pagination, data = data)
                }

            call.respondWithStatusResponse(result)
        }
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = tiltaksgjennomforingService.get(id)
                .map { TiltaksgjennomforingDto.from(it) }

            call.respondWithStatusResponse(result)
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

            val result = tiltaksgjennomforingService.get(id)
                .flatMap { gjennomforing ->
                    arenaAdapterService.hentTiltaksgjennomforingsstatus(id)
                        ?.let { TiltaksgjennomforingsArenadataDto.from(gjennomforing, it.status).right() }
                        ?: NotFound("Det finnes ikke noe tiltaksgjennomføring med id=$id").left()
                }

            call.respondWithStatusResponse(result)
        }
    }
}
