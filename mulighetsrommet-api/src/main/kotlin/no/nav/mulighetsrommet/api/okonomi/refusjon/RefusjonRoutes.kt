package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.ktor.server.application.*
import io.ktor.server.request.*
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
            val orgnr = call.parameters.getOrFail<Organisasjonsnummer>("orgnr")

            call.respond(service.getByOrgnr(orgnr))
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
