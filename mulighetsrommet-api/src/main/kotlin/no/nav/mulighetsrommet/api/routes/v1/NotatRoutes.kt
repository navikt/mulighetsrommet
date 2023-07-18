package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import arrow.core.flatMap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.AvtaleNotatService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingNotatService
import no.nav.mulighetsrommet.api.utils.getAvtaleNotatFilter
import no.nav.mulighetsrommet.api.utils.getTiltaksgjennomforingNotatFlter
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.avtaleNotatRoutes() {
    val avtaleNotatService: AvtaleNotatService by inject()
    val tiltaksgjennomforingNotatService: TiltaksgjennomforingNotatService by inject()
    val logger = application.environment.log

    route("/api/v1/internal/notater") {
        get("avtaler") {
            val filter = getAvtaleNotatFilter()
            val result = avtaleNotatService.getAll(filter = filter)

            call.respondWithStatusResponse(result)
        }

        get("avtaler/mine") {
            val filter = getAvtaleNotatFilter()
            val result = avtaleNotatService.getAll(filter = filter.copy(opprettetAv = getNavIdent()))

            call.respondWithStatusResponse(result)
        }
        get("avtaler/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            avtaleNotatService.get(id)
                .onRight {
                    if (it == null) {
                        return@get call.respondText(
                            text = "Det finnes ikke noe notat for avtale med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    return@get call.respond(it)
                }
                .onLeft {
                    log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente notat for avtale med id: '$id'")
                }
        }

        put("avtaler") {
            val avtaleRequest = call.receive<AvtaleNotatRequest>()

            val result = avtaleRequest.copy(opprettetAv = getNavIdent()).toDbo()
                .flatMap { avtaleNotatService.upsert(it) }
                .onLeft { logger.error(it.message) }

            call.respondWithStatusResponse(result)
        }

        delete("avtaler/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()
            call.respondWithStatusResponse(avtaleNotatService.delete(id, navIdent))
        }

        get("tiltaksgjennomforinger") {
            val filter = getTiltaksgjennomforingNotatFlter()
            val result = tiltaksgjennomforingNotatService.getAll(filter = filter)

            call.respondWithStatusResponse(result)
        }

        get("tiltaksgjennomforinger/mine") {
            val filter = getTiltaksgjennomforingNotatFlter()
            val result = tiltaksgjennomforingNotatService.getAll(filter = filter.copy(opprettetAv = getNavIdent()))

            call.respondWithStatusResponse(result)
        }
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            tiltaksgjennomforingNotatService.get(id)
                .onRight {
                    if (it == null) {
                        return@get call.respondText(
                            text = "Det finnes ikke noe notat for tiltaksgjennomføring med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    return@get call.respond(it)
                }
                .onLeft {
                    log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente notat for tiltaksgjennomføring med id: '$id'")
                }
        }

        put("tiltaksgjennomforinger") {
            val tiltaksgjennomforingNotatRequest = call.receive<TiltaksgjennomforingNotatRequest>()

            val result = tiltaksgjennomforingNotatRequest.copy(opprettetAv = getNavIdent()).toDbo()
                .flatMap { tiltaksgjennomforingNotatService.upsert(it) }
                .onLeft { logger.error(it.message) }

            call.respondWithStatusResponse(result)
        }

        delete("tiltaksgjennomforinger/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()
            call.respondWithStatusResponse(tiltaksgjennomforingNotatService.delete(id, navIdent))
        }
    }
}

@Serializable
data class AvtaleNotatRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val opprettetAv: String,
    val innhold: String,
) {
    fun toDbo(): StatusResponse<AvtaleNotatDbo> {
        return Either.Right(
            AvtaleNotatDbo(
                id = id,
                avtaleId = avtaleId,
                opprettetAv = opprettetAv,
                innhold = innhold,
                createdAt = null,
                updatedAt = null,
            ),
        )
    }
}

@Serializable
data class TiltaksgjennomforingNotatRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
    val opprettetAv: String,
    val innhold: String,
) {
    fun toDbo(): StatusResponse<TiltaksgjennomforingNotatDbo> {
        return Either.Right(
            TiltaksgjennomforingNotatDbo(
                id = id,
                tiltaksgjennomforingId = tiltaksgjennomforingId,
                opprettetAv = opprettetAv,
                innhold = innhold,
                createdAt = null,
                updatedAt = null,
            ),
        )
    }
}
