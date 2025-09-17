package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AvtaltSatsDto(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate,
    val pris: Int,
    val valuta: String,
    @Serializable(with = LocalDateSerializer::class)
    val gjelderTil: LocalDate? = null,
) {
    companion object {
        fun fromAvtaltSats(avtaltSats: AvtaltSats, nesteSats: AvtaltSats? = null): AvtaltSatsDto {
            return AvtaltSatsDto(
                gjelderFra = avtaltSats.gjelderFra,
                pris = avtaltSats.sats,
                valuta = "NOK",
                gjelderTil = nesteSats?.gjelderFra?.minusDays(1),
            )
        }
    }
}

fun List<AvtaltSats>.toDto(): List<AvtaltSatsDto> {
    val mappedList = mutableListOf<AvtaltSatsDto>()
    this.windowed(size = 2, partialWindows = true).map { sats ->
        val nextSats = sats.getOrNull(1)
        mappedList.add(AvtaltSatsDto.fromAvtaltSats(sats[0], nextSats))
    }
    return mappedList
}
