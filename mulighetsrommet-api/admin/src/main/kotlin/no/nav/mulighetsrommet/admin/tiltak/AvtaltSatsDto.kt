package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AvtaltSatsDto(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate,
    val pris: ValutaBelop,
    @Serializable(with = LocalDateSerializer::class)
    val gjelderTil: LocalDate? = null,
) {
    companion object {
        fun fromAvtaltSats(
            avtaltSats: AvtaltSats,
            nesteSats: AvtaltSats? = null,
        ): AvtaltSatsDto {
            return AvtaltSatsDto(
                gjelderFra = avtaltSats.gjelderFra,
                pris = avtaltSats.sats,
                gjelderTil = nesteSats?.gjelderFra?.minusDays(1),
            )
        }
    }
}
