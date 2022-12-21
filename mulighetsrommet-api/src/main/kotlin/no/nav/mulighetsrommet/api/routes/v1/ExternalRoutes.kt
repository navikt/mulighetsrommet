package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.toUUID
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*

fun Route.externalRoutes() {
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()

    route("/api/v1/external") {
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing =
                tiltaksgjennomforinger.getForExternalWithTiltakstypedata(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltaksgjennomforing)
        }
    }
}

@Serializable
data class TiltaksgjennomforingMedTiltakstypeEkstern(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: TiltakstypeEkstern,
    val navn: String,
    @Serializable(with = DateSerializer::class)
    val startDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val sluttDato: LocalDateTime? = null
)

@Serializable
data class TiltakstypeEkstern(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String
)
