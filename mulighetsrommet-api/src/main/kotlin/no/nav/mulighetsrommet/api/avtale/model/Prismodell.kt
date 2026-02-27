package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class Prismodell {
    abstract val id: UUID
    abstract val type: PrismodellType
    abstract val valuta: Valuta

    @Serializable
    data class AnnenAvtaltPris(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.ANNEN_AVTALT_PRIS
    }

    @Serializable
    data class AnnenAvtaltPrisPerDeltaker(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.ANNEN_AVTALT_PRIS_PER_DELTAKER
    }

    @Serializable
    data class ForhandsgodkjentPrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class AvtaltPrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class AvtaltPrisPerUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerHeleUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
    }

    companion object {
        fun from(type: PrismodellType, id: UUID, valuta: Valuta, prisbetingelser: String?, satser: List<AvtaltSats>?): Prismodell {
            val satser = (satser ?: listOf()).windowed(size = 2, partialWindows = true).map { sats ->
                val nextSats = sats.getOrNull(1)
                AvtaltSatsDto.fromAvtaltSats(sats[0], nextSats)
            }
            return when (type) {
                PrismodellType.ANNEN_AVTALT_PRIS -> AnnenAvtaltPris(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                )

                PrismodellType.ANNEN_AVTALT_PRIS_PER_DELTAKER -> AnnenAvtaltPrisPerDeltaker(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                )

                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> ForhandsgodkjentPrisPerManedsverk(
                    id = id,
                    valuta = valuta,
                    satser = satser,
                )

                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK -> AvtaltPrisPerManedsverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = satser,
                )

                PrismodellType.AVTALT_PRIS_PER_UKESVERK -> AvtaltPrisPerUkesverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = satser,
                )

                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK -> AvtaltPrisPerHeleUkesverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = satser,
                )

                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> AvtaltPrisPerTimeOppfolgingPerDeltaker(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = satser,
                )
            }
        }
    }
}

fun List<AvtaltSats>.findAvtaltSats(dato: LocalDate): AvtaltSats? = this
    .sortedBy { it.gjelderFra }
    .lastOrNull { dato >= it.gjelderFra }
