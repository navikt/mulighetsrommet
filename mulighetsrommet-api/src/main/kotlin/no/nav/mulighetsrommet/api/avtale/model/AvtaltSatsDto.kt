package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AvtaltSatsDto(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate,
    val pris: Int,
    val valuta: Valuta,
    @Serializable(with = LocalDateSerializer::class)
    val gjelderTil: LocalDate? = null,
) {
    companion object {
        fun fromAvtaltSats(avtaltSats: AvtaltSats, nesteSats: AvtaltSats? = null): AvtaltSatsDto {
            return AvtaltSatsDto(
                gjelderFra = avtaltSats.gjelderFra,
                pris = avtaltSats.sats,
                valuta = avtaltSats.valuta,
                gjelderTil = nesteSats?.gjelderFra?.minusDays(1),
            )
        }
    }
}
