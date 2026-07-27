package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
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
    val tilskudd: List<TilskuddOgBelop>,
    val totalBelop: Int?,
    val aarsak: Prismodell.IngenKostnader.Aarsak?,
) {
    val navn: String = type.navn
    val beskrivelse: List<String> = type.beskrivelse

    @Serializable
    data class TilskuddOgBelop(
        val type: Opplaeringtilskudd.Kode,
        val belop: Int,
    )
}

fun Prismodell.toPrismodellDto(): PrismodellDto {
    val tilsagnPerDeltaker = when (this) {
        is Prismodell.AnnenAvtaltPris -> tilsagnPerDeltaker

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

    val tilskudd = when (this) {
        is Prismodell.TilskuddTilOpplaering -> tilskudd.map {
            PrismodellDto.TilskuddOgBelop(it.key, it.value)
        }.toList()

        is Prismodell.AnnenAvtaltPris,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
        is Prismodell.FastSatsPerAvtaltPlassPerManed,
        is Prismodell.FastSatsPerBenyttetPlassPerManed,
        is Prismodell.IngenKostnader,
        -> emptyList()
    }

    val totalbelop = when (this) {
        is Prismodell.AnnenAvtaltPris -> totalbelop

        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
        is Prismodell.FastSatsPerAvtaltPlassPerManed,
        is Prismodell.FastSatsPerBenyttetPlassPerManed,
        is Prismodell.IngenKostnader,
        is Prismodell.TilskuddTilOpplaering,
        -> null
    }

    val aarsak = when (this) {
        is Prismodell.IngenKostnader -> aarsak

        is Prismodell.AnnenAvtaltPris,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
        is Prismodell.FastSatsPerAvtaltPlassPerManed,
        is Prismodell.FastSatsPerBenyttetPlassPerManed,
        is Prismodell.TilskuddTilOpplaering,
        -> null
    }

    return PrismodellDto(
        id = id,
        type = type,
        valuta = valuta,
        satser = satser().windowed(size = 2, partialWindows = true).map { sats ->
            AvtaltSatsDto.fromAvtaltSats(sats[0], sats.getOrNull(1))
        },
        prisbetingelser = prisbetingelser(),
        tilsagnPerDeltaker = tilsagnPerDeltaker,
        tilskudd = tilskudd,
        totalBelop = totalbelop,
        aarsak = aarsak,
    )
}
