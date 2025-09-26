package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Prismodell {
    abstract val type: PrismodellType

    @Serializable
    data class AnnenAvtaltPris(
        val prisbetingelser: String?,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.ANNEN_AVTALT_PRIS
    }

    @Serializable
    data object ForhandsgodkjentPrisPerManedsverk : Prismodell() {
        @Transient
        override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class AvtaltPrisPerManedsverk(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    data class AvtaltPrisPerUkesverk(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerHeleUkesverk(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK
    }

    @Serializable
    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : Prismodell() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
    }
}
