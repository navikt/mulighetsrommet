package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class PrismodellDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: PrismodellType,
    val satser: List<AvtaltSatsDto>?,
    val valuta: Valuta,
    val prisbetingelser: String?,
    val tilsagnPerDeltaker: Boolean?,
) {
    val navn: String = type.navn
    val beskrivelse: List<String> = type.beskrivelse
}

fun fromPrismodell(prismodell: Prismodell): PrismodellDto {
    val satser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> null
        is Prismodell.FastSatsPerBenyttetPlassPerManed -> prismodell.satser
        is Prismodell.FastSatsPerAvtaltPlassPerManed -> prismodell.satser
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed -> prismodell.satser
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke -> prismodell.satser
        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke -> prismodell.satser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.satser
        is Prismodell.TilskuddTilOpplaering -> null
        is Prismodell.IngenKostnader -> null
    }

    val prisbetingelser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.prisbetingelser
        is Prismodell.FastSatsPerBenyttetPlassPerManed -> null
        is Prismodell.FastSatsPerAvtaltPlassPerManed -> null
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.prisbetingelser
        is Prismodell.TilskuddTilOpplaering -> prismodell.tilleggsopplysninger
        is Prismodell.IngenKostnader -> prismodell.tilleggsopplysninger
    }

    val tilsagnPerDeltaker = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.tilsagnPerDeltaker

        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
        is Prismodell.FastSatsPerAvtaltPlassPerManed,
        is Prismodell.FastSatsPerBenyttetPlassPerManed,
        is Prismodell.TilskuddTilOpplaering,
        is Prismodell.IngenKostnader,
        -> null
    }

    return PrismodellDto(
        id = prismodell.id,
        type = prismodell.type,
        valuta = prismodell.valuta,
        satser = (satser ?: listOf()).windowed(size = 2, partialWindows = true).map { sats ->
            AvtaltSatsDto.fromAvtaltSats(sats[0], sats.getOrNull(1))
        },
        prisbetingelser = prisbetingelser,
        tilsagnPerDeltaker = tilsagnPerDeltaker,
    )
}
