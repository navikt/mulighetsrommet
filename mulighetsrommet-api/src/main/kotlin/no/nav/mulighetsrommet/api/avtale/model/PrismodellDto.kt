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
) {
    val navn: String = type.navn
    val beskrivelse: List<String> = type.beskrivelse
}

fun fromPrismodell(prismodell: Prismodell): PrismodellDto {
    val satser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> null
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerManedsverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerUkesverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.satser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.satser
    }
    val prisbetingelser = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.prisbetingelser
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
        is Prismodell.AvtaltPrisPerManedsverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerUkesverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.prisbetingelser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.prisbetingelser
    }
    val valuta = when (prismodell) {
        is Prismodell.AnnenAvtaltPris -> prismodell.valuta
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> prismodell.satser.first().pris.valuta
        is Prismodell.AvtaltPrisPerManedsverk -> prismodell.satser.first().pris.valuta
        is Prismodell.AvtaltPrisPerUkesverk -> prismodell.satser.first().pris.valuta
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.satser.first().pris.valuta
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.satser.first().pris.valuta
    }
    return PrismodellDto(
        id = prismodell.id,
        type = prismodell.type,
        satser = satser,
        prisbetingelser = prisbetingelser,
        valuta = valuta,
    )
}
