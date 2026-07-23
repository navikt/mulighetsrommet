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
    val satser = when (this) {
        is Prismodell.AnnenAvtaltPris -> null
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> satser
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> satser
        is Prismodell.AvtaltPrisPerManedsverk -> satser
        is Prismodell.AvtaltPrisPerUkesverk -> satser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> satser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> satser
        is Prismodell.TilskuddTilOpplaering -> null
        is Prismodell.IngenKostnader -> null
    }

    val prisbetingelser = when (this) {
        is Prismodell.AnnenAvtaltPris -> prisbetingelser
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> null
        is Prismodell.AvtaltPrisPerManedsverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerUkesverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
        is Prismodell.TilskuddTilOpplaering -> tilleggsopplysninger
        is Prismodell.IngenKostnader -> tilleggsopplysninger
    }

    val tilsagnPerDeltaker = when (this) {
        is Prismodell.AnnenAvtaltPris -> tilsagnPerDeltaker

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

    val tilskudd = when (this) {
        is Prismodell.TilskuddTilOpplaering -> tilskudd.map {
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

    val totalbelop = when (this) {
        is Prismodell.AnnenAvtaltPris -> totalbelop

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

    val aarsak = when (this) {
        is Prismodell.IngenKostnader -> aarsak

        is Prismodell.AvtaltPrisPerHeleUkesverk,
        is Prismodell.AvtaltPrisPerManedsverk,
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerUkesverk,
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass,
        is Prismodell.ForhandsgodkjentPrisPerManedsverk,
        is Prismodell.AnnenAvtaltPris,
        is Prismodell.TilskuddTilOpplaering,
        -> null
    }

    return PrismodellDto(
        id = id,
        type = type,
        valuta = valuta,
        satser = (satser ?: listOf()).windowed(size = 2, partialWindows = true).map { sats ->
            AvtaltSatsDto.fromAvtaltSats(sats[0], sats.getOrNull(1))
        },
        prisbetingelser = prisbetingelser,
        tilsagnPerDeltaker = tilsagnPerDeltaker,
        tilskudd = tilskudd,
        totalBelop = totalbelop,
        aarsak = aarsak,
    )
}
