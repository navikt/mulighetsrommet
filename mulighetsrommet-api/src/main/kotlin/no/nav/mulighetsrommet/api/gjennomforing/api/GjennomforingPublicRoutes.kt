package no.nav.mulighetsrommet.api.gjennomforing.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.model.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.model.TiltaksgjennomforingArenaDataDto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import org.koin.ktor.ext.inject
import java.util.*

fun Route.gjennomforingPublicRoutes() {
    route("/v2/tiltaksgjennomforinger") {
        getGjennomforingV2Route()
    }

    route("/v1/tiltaksgjennomforinger") {
        getGjennomforingV1Route()
        getGjennomforingIdRoute()
        getGjennomforingArenaDataRoute()
    }
}

private fun Route.getGjennomforingV2Route() {
    val db: ApiDatabase by inject()

    suspend fun QueryContext.getTiltaksgjennomforingV2(id: UUID): TiltaksgjennomforingV2Dto? = coroutineScope {
        val gruppe = async {
            queries.gjennomforing.get(id)?.let(TiltaksgjennomforingV2Mapper::fromGruppe)
        }

        val enkeltplass = async {
            queries.enkeltplass.get(id)?.let(TiltaksgjennomforingV2Mapper::fromEnkeltplass)
        }

        listOf(gruppe, enkeltplass).awaitAll().firstOrNull { it != null }
    }

    get("{id}", {
        tags = setOf("Tiltaksgjennomforing")
        operationId = "getTiltaksgjennomforingV2"
        request {
            pathParameterUuid("id")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Gjennomføringen"
                body<TiltaksgjennomforingV2Dto>()
            }
            code(HttpStatusCode.NotFound) {
                description = "Gjennomføringen ble ikke funnet"
            }
        }
    }) {
        val id: UUID by call.parameters

        val tiltaksgjennomforing = db.session { getTiltaksgjennomforingV2(id) }
            ?: return@get call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")

        call.respond(tiltaksgjennomforing)
    }
}

private fun Route.getGjennomforingV1Route() {
    val gjennomforingService: GjennomforingService by inject()

    get("{id}", {
        tags = setOf("Tiltaksgjennomforing")
        operationId = "getTiltaksgjennomforingV1"
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
}

private fun Route.getGjennomforingIdRoute() {
    val arenaAdapterService: ArenaAdapterClient by inject()

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
}

private fun Route.getGjennomforingArenaDataRoute() {
    val db: ApiDatabase by inject()

    fun toArenaDataDto(tiltaksnummer: String): TiltaksgjennomforingArenaDataDto {
        val (aar, lopenr) = tiltaksnummer.split("#")
        return TiltaksgjennomforingArenaDataDto(opprettetAar = aar.toInt(), lopenr = lopenr.toInt())
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
        val id: UUID by call.parameters

        val tiltaksnummer = db.session {
            val gjennomoforing = queries.gjennomforing.get(id)
            val enkeltplass = queries.enkeltplass.get(id)

            if (gjennomoforing == null && enkeltplass == null) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            gjennomoforing?.tiltaksnummer ?: enkeltplass?.arena?.tiltaksnummer
        }

        val arenaData = tiltaksnummer?.let { toArenaDataDto(it) }
            ?: return@get call.respond(HttpStatusCode.NoContent)

        call.respond(HttpStatusCode.OK, arenaData)
    }
}
