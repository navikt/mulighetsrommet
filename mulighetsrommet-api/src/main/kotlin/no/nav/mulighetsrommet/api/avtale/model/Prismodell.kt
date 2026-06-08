package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed interface Prismodell {
    val id: UUID
    val type: PrismodellType
    val valuta: Valuta

    @Serializable
    data class AnnenAvtaltPris(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val tilsagnPerDeltaker: Boolean,
        val prisbetingelser: String?,
        val totalbelop: Int?,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.ANNEN_AVTALT_PRIS
    }

    @Serializable
    data class ForhandsgodkjentPrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class ForhandsgodkjentPrisPerAvtaltTiltaksplass(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS
    }

    @Serializable
    data class AvtaltPrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class AvtaltPrisPerUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerHeleUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
    }

    @Serializable
    data class TilskuddTilOpplaering(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val tilskudd: Map<Opplaeringtilskudd.Kode, Int>,
        val tilleggsopplysninger: String?,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.TILSKUDD_TIL_OPPLAERING
    }

    @Serializable
    data class IngenKostnader(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val aarsak: Aarsak,
        val tilleggsopplysninger: String?,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.INGEN_KOSTNADER

        enum class Aarsak {
            OPPLAERINGEN_ER_KOSTNADSFRI,
            OPPLAERINGEN_ER_EGENFINANSIERT,
        }
    }

    fun findAvtaltSats(dato: LocalDate): AvtaltSats? {
        return satser()
            .sortedBy { it.gjelderFra }
            .lastOrNull { dato >= it.gjelderFra }
    }

    companion object {
        fun from(
            type: PrismodellType,
            id: UUID,
            valuta: Valuta,
            prisbetingelser: String?,
            satser: List<AvtaltSats>?,
            tilsagnPerDeltaker: Boolean?,
            totalbelop: Int? = null,
            tilskudd: Map<Opplaeringtilskudd.Kode, Int>? = null,
            aarsak: String? = null,
        ): Prismodell {
            return when (type) {
                PrismodellType.ANNEN_AVTALT_PRIS -> AnnenAvtaltPris(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    tilsagnPerDeltaker = requireNotNull(tilsagnPerDeltaker),
                    totalbelop = totalbelop,
                )

                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> ForhandsgodkjentPrisPerManedsverk(
                    id = id,
                    valuta = valuta,
                    satser = requireNotNull(satser),
                )

                PrismodellType.FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS -> ForhandsgodkjentPrisPerAvtaltTiltaksplass(
                    id = id,
                    valuta = valuta,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK -> AvtaltPrisPerManedsverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_UKESVERK -> AvtaltPrisPerUkesverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK -> AvtaltPrisPerHeleUkesverk(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.INGEN_KOSTNADER -> IngenKostnader(
                    id = id,
                    valuta = valuta,
                    tilleggsopplysninger = prisbetingelser,
                    aarsak = IngenKostnader.Aarsak.valueOf(requireNotNull(aarsak)),
                )

                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> AvtaltPrisPerTimeOppfolgingPerDeltaker(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.TILSKUDD_TIL_OPPLAERING -> TilskuddTilOpplaering(
                    id = id,
                    valuta = valuta,
                    tilleggsopplysninger = prisbetingelser,
                    tilskudd = requireNotNull(tilskudd),
                )
            }
        }
    }
}
