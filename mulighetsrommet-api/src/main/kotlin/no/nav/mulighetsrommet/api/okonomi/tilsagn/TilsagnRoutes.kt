package no.nav.mulighetsrommet.api.okonomi.tilsagn

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tilsagnRoutes() {
    val service: TilsagnService by inject()

    route("/api/v1/intern/tilsagn") {
        get("/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = service.get(id) ?: NotFound()

            call.respond(result)
        }

        authenticate(
            AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            put {
                val request = call.receive<TilsagnRequest>()
                val navIdent = getNavIdent()

                val result = service.upsert(request, navIdent)
                    .mapLeft { BadRequest(errors = it) }

                call.respondWithStatusResponse(result)
            }

            delete("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                call.respondWithStatusResponse(service.annuller(id))
            }
        }

        authenticate(
            AuthProvider.AZURE_AD_OKONOMI_BESLUTTER.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            post("/{id}/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttTilsagnRequest>()
                val navIdent = getNavIdent()

                call.respondWithStatusResponse(service.beslutt(id, request.besluttelse, navIdent))
            }
        }
    }

    route("/api/v1/intern/tiltaksgjennomforinger/{tiltaksgjennomforingId}/tilsagn") {
        authenticate(
            AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            get {
                val tiltaksgjennomforingId = call.parameters.getOrFail<UUID>("tiltaksgjennomforingId")

                val result = service.getByGjennomforingId(tiltaksgjennomforingId)

                call.respond(result)
            }
        }
    }
}

@Serializable
data class TilsagnRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val belop: Int,
) {
    fun toDbo(opprettetAv: NavIdent, arrangorId: UUID) = TilsagnDbo(
        id = id,
        tiltaksgjennomforingId = tiltaksgjennomforingId,
        periodeStart = periodeStart,
        periodeSlutt = periodeSlutt,
        kostnadssted = kostnadssted,
        belop = belop,
        opprettetAv = opprettetAv,
        arrangorId = arrangorId,
    )
}

@Serializable
data class BesluttTilsagnRequest(
    val besluttelse: TilsagnBesluttelse,
)

enum class TilsagnBesluttelse {
    GODKJENT,
    AVVIST,
}
