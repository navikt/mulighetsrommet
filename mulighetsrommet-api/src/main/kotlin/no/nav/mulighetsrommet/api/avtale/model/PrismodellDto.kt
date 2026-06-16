package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
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
) {
    val navn: String = type.navn
    val beskrivelse: List<String> = type.beskrivelse

    @Serializable
    data class TilskuddOgBelop(
        val type: Opplaeringtilskudd.Kode,
        val belop: Int,
    )
}

fun fromPrismodell(prismodell: Prismodell): PrismodellDto {
    val satser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> null
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> prismodell.satser
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> prismodell.satser
        is Prismodell.AvtaltPrisPerManedsverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerUkesverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.satser
        is Prismodell.TilskuddTilOpplaering -> null
        is Prismodell.IngenKostnader -> null
    }

    val prisbetingelser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.prisbetingelser
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> null
        is Prismodell.AvtaltPrisPerManedsverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerUkesverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.prisbetingelser
        is Prismodell.TilskuddTilOpplaering -> prismodell.tilleggsopplysninger
        is Prismodell.IngenKostnader -> prismodell.tilleggsopplysninger
    }

    val tilsagnPerDeltaker = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.tilsagnPerDeltaker

        is Prismodell.AvtaltPrisPerHeleUkesverk,
        is Prismodell.AvtaltPrisPerManedsverk,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerUkesverk,
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass,
        is Prismodell.ForhandsgodkjentPrisPerManedsverk,
        is Prismodell.TilskuddTilOpplaering,
        is Prismodell.IngenKostnader,
        -> null
    }
    val tilskudd = when (prismodell) {
        is Prismodell.TilskuddTilOpplaering -> prismodell.tilskudd.map {
            PrismodellDto.TilskuddOgBelop(it.key, it.value)
        }.toList()

        is Prismodell.AnnenAvtaltPris,
        is Prismodell.AvtaltPrisPerHeleUkesverk,
        is Prismodell.AvtaltPrisPerManedsverk,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerUkesverk,
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass,
        is Prismodell.ForhandsgodkjentPrisPerManedsverk,
        is Prismodell.IngenKostnader,
        -> emptyList()
    }
    val totalBelop = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.totalbelop

        is Prismodell.AvtaltPrisPerHeleUkesverk,
        is Prismodell.AvtaltPrisPerManedsverk,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerUkesverk,
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass,
        is Prismodell.ForhandsgodkjentPrisPerManedsverk,
        is Prismodell.IngenKostnader,
        is Prismodell.TilskuddTilOpplaering,
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
        tilskudd = tilskudd,
        totalBelop = totalBelop,
    )
}
