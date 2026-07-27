package no.nav.mulighetsrommet.api.domain.tiltak

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
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
    data class FastSatsPerBenyttetPlassPerManed(
        /**
         * Prismodeller for forhåndsgodkjente tiltak er identifisert med en kjent system-id.
         * Dette tillater systemet å finne riktig prismodell for alle tiltak for en gitt tiltakskode.
         */
        val systemId: String? = null,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED
    }

    @Serializable
    data class FastSatsPerAvtaltPlassPerManed(
        /**
         * Prismodeller for forhåndsgodkjente tiltak er identifisert med en kjent system-id.
         * Dette tillater systemet å finne riktig prismodell for alle tiltak for en gitt tiltakskode.
         */
        val systemId: String? = null,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED
    }

    @Serializable
    data class AvtaltPrisPerBenyttetPlassPerManed(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED
    }

    @Serializable
    data class AvtaltPrisPerBenyttetPlassPerUke(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE
    }

    @Serializable
    data class AvtaltPrisPerBenyttetPlassPerHeleUke(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val valuta: Valuta,
        val prisbetingelser: String?,
        val satser: List<AvtaltSats>,
    ) : Prismodell {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE
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

    fun satser(): List<AvtaltSats> = when (this) {
        is AnnenAvtaltPris -> emptyList()
        is AvtaltPrisPerBenyttetPlassPerManed -> satser
        is AvtaltPrisPerBenyttetPlassPerUke -> satser
        is AvtaltPrisPerBenyttetPlassPerHeleUke -> satser
        is AvtaltPrisPerTimeOppfolgingPerDeltaker -> satser
        is FastSatsPerBenyttetPlassPerManed -> satser
        is FastSatsPerAvtaltPlassPerManed -> satser
        is TilskuddTilOpplaering -> emptyList()
        is IngenKostnader -> emptyList()
    }

    // TODO: enten behandle "prisbetingelser" og "tilleggsopplysninger" som to separate felter, eller benytte samme navn
    fun prisbetingelser(): String? = when (this) {
        is AnnenAvtaltPris -> prisbetingelser
        is AvtaltPrisPerBenyttetPlassPerManed -> prisbetingelser
        is AvtaltPrisPerBenyttetPlassPerUke -> prisbetingelser
        is AvtaltPrisPerBenyttetPlassPerHeleUke -> prisbetingelser
        is AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
        is FastSatsPerBenyttetPlassPerManed -> null
        is FastSatsPerAvtaltPlassPerManed -> null
        is TilskuddTilOpplaering -> tilleggsopplysninger
        is IngenKostnader -> tilleggsopplysninger
    }

    fun findAvtaltSats(dato: LocalDate): AvtaltSats? {
        return satser()
            .sortedBy { it.gjelderFra }
            .lastOrNull { dato >= it.gjelderFra }
    }

    companion object {
        fun from(
            id: UUID,
            type: PrismodellType,
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

                PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED -> FastSatsPerBenyttetPlassPerManed(
                    id = id,
                    valuta = valuta,
                    satser = requireNotNull(satser),
                )

                PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED -> FastSatsPerAvtaltPlassPerManed(
                    id = id,
                    valuta = valuta,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED -> AvtaltPrisPerBenyttetPlassPerManed(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE -> AvtaltPrisPerBenyttetPlassPerUke(
                    id = id,
                    valuta = valuta,
                    prisbetingelser = prisbetingelser,
                    satser = requireNotNull(satser),
                )

                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE -> AvtaltPrisPerBenyttetPlassPerHeleUke(
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
