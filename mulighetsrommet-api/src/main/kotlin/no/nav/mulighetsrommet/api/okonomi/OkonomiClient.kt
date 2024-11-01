package no.nav.mulighetsrommet.api.okonomi

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

object OkonomiClient {
    suspend fun sendUtbetaling(utbetaling: UtbetalingDto) {
        Unit // TODO: Not implemented
    }

    suspend fun sendBestilling(bestilling: BestillingDto) {
        Unit // TODO: Not implemented
    }

    suspend fun annullerOrder(okonomiId: String) {
        Unit // TODO: Not implemented
    }
}

@Serializable
data class UtbetalingDto(
    val okonomiId: String,
)

@Serializable
data class BestillingDto(
    val okonomiId: String,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val organisasjonsnummer: Organisasjonsnummer,
    val kostnadSted: NavEnhetDbo,
    val belop: Int,
)
