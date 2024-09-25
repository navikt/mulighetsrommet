package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.refusjonRoutes() {
    val service: RefusjonService by inject()

    route("/api/v1/intern/refusjon") {
        get("/{orgnr}/krav") {
            val orgnr = Organisasjonsnummer(call.parameters.getOrFail<String>("orgnr"))

            call.respond(service.getByOrgnr(orgnr))
        }
        get("/krav/{id}") {
            // val orgnr = Organisasjonsnummer(call.parameters.getOrFail<String>("orgnr"))
            val id = call.parameters.getOrFail<UUID>("id")
            val krav = service.getById(id)

            if (krav != null) {
                call.respond(krav)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

@Serializable
data class RefusjonskravDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksgjennomforing: Gjennomforing,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val beregning: Prismodell.RefusjonskravBeregning,
    val arrangor: Arrangor,
) {
    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: String,
        val navn: String,
        val slettet: Boolean,
    )
}
