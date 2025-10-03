package no.nav.mulighetsrommet.api.gjennomforing.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.model.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.model.TiltaksgjennomforingArenaDataDto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.gjennomforingPublicRoutes() {
    val gjennomforingService: GjennomforingService by inject()
    val arenaAdapterService: ArenaAdapterClient by inject()

    route("/v1/tiltaksgjennomforinger") {
        get("{id}", {
            tags = setOf("Tiltaksgjennomforing")
            operationId = "getTiltaksgjennomforing"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringen"
                    body<TiltaksgjennomforingV1Dto>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Gjennomføringen ble ikke funnet"
                }
            }
        }) {
            val id: UUID by call.parameters

            val result = gjennomforingService.get(id)
                ?.let { TiltaksgjennomforingV1Mapper.fromGjennomforing(it) }
                ?: return@get call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")

            call.respond(result)
        }

        get("id/{arenaId}", {
            tags = setOf("Tiltaksgjennomforing")
            operationId = "getTiltaksgjennomforingId"
            request {
                pathParameter<String>("arenaId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Id til tiltaksgjennomføringen"
                    body<ExchangeArenaIdForIdResponse>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Gjennomføringen ble ikke funnet"
                }
            }
        }) {
            val arenaId: String by call.parameters

            val response = arenaAdapterService.exchangeTiltaksgjennomforingArenaIdForId(arenaId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    "Det finnes ingen tiltaksgjennomføring med arenaId=$arenaId",
                )

            call.respond(response)
        }

        get("arenadata/{id}", {
            tags = setOf("Tiltaksgjennomforing")
            operationId = "getTiltaksgjennomforingArenadata"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Arenadata for tiltaksgjennføringen"
                    body<TiltaksgjennomforingArenaDataDto>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Gjennomføringen ble ikke funnet"
                }
            }
        }) {
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
