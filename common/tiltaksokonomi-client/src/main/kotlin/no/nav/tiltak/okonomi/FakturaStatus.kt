package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: FakturaStatusType,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fakturaStatusSistOppdatert: LocalDateTime?,
)

enum class FakturaStatusType {
    SENDT,
    UTBETALT,
    FEILET,
}
