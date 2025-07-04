package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AvtaltSatsDto(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val pris: Int,
    val valuta: String,
) {
    companion object {
        fun fromAvtaltSats(avtaltSats: AvtaltSats): AvtaltSatsDto {
            return AvtaltSatsDto(
                periodeStart = avtaltSats.periode.start,
                periodeSlutt = avtaltSats.periode.getLastInclusiveDate(),
                pris = avtaltSats.sats,
                valuta = "NOK",
            )
        }
    }
}
