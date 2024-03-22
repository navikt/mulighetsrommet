package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import arrow.core.flatMap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.NotatService
import no.nav.mulighetsrommet.api.utils.getNotatFilter
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.avtaleNotatRoutes() {
    val notatService: NotatService by inject()
    val logger = application.environment.log

    route("/api/v1/internal/notater") {
        authenticate(AuthProvider.AZURE_AD_AVTALER_SKRIV.name, strategy = AuthenticationStrategy.Required) {
            put("avtaler") {
                val avtaleRequest = call.receive<AvtaleNotatRequest>()

                val result = avtaleRequest.copy(opprettetAv = getNavIdent()).toDbo()
                    .flatMap { notatService.upsertAvtaleNotat(it) }
                    .onLeft { logger.error(it.message) }

                call.respondWithStatusResponse(result)
            }

            delete("avtaler/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                call.respondWithStatusResponse(notatService.deleteAvtaleNotat(id, navIdent))
            }
        }

        authenticate(
            AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            put("tiltaksgjennomforinger") {
                val tiltaksgjennomforingNotatRequest = call.receive<TiltaksgjennomforingNotatRequest>()

                val result = tiltaksgjennomforingNotatRequest.copy(opprettetAv = getNavIdent()).toDbo()
                    .flatMap { notatService.upsertTiltaksgjennomforingNotat((it)) }
                    .onLeft { logger.error(it.message) }

                call.respondWithStatusResponse(result)
            }

            delete("tiltaksgjennomforinger/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                call.respondWithStatusResponse(notatService.deleteTiltaksgjennomforingNotat(id, navIdent))
            }
        }

        get("avtaler") {
            val filter = getNotatFilter()
            val result = notatService.getAllAvtaleNotater(filter = filter)

            call.respondWithStatusResponse(result)
        }

        get("avtaler/mine") {
            val filter = getNotatFilter()
            val result = notatService.getAllAvtaleNotater(filter = filter.copy(opprettetAv = getNavIdent()))

            call.respondWithStatusResponse(result)
        }
        get("avtaler/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            notatService.getAvtaleNotat(id)
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
                    application.log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente notat for avtale med id: '$id'")
                }
        }

        get("tiltaksgjennomforinger") {
            val result = notatService.getAllTiltaksgjennomforingNotater(filter = getNotatFilter())

            call.respondWithStatusResponse(result)
        }

        get("tiltaksgjennomforinger/mine") {
            val result =
                notatService.getAllTiltaksgjennomforingNotater(filter = getNotatFilter().copy(opprettetAv = getNavIdent()))

            call.respondWithStatusResponse(result)
        }
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            notatService.getTiltaksgjennomforingNotat(id)
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
                    application.log.error("$it")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Kunne ikke hente notat for tiltaksgjennomføring med id: '$id'",
                    )
                }
        }
    }
}

@Serializable
data class AvtaleNotatRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val opprettetAv: NavIdent? = null,
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
    val opprettetAv: NavIdent? = null,
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
