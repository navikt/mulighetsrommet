package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class PrismodellDto {
    abstract val type: PrismodellType

    @Serializable
    @SerialName("ANNEN_AVTALT_PRIS")
    data class AnnenAvtaltPris(
        val prisbetingelser: String?,
    ) : PrismodellDto() {
        @Transient
        override val type = PrismodellType.ANNEN_AVTALT_PRIS
    }

    @Serializable
    @SerialName("FORHANDSGODKJENT_PRIS_PER_MANEDSVERK")
    data object ForhandsgodkjentPrisPerManedsverk : PrismodellDto() {
        @Transient
        override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    @SerialName("AVTALT_PRIS_PER_MANEDSVERK")
    data class AvtaltPrisPerManedsverk(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : PrismodellDto() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    }

    @Serializable
    @SerialName("AVTALT_PRIS_PER_UKESVERK")
    data class AvtaltPrisPerUkesverk(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : PrismodellDto() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    }

    @Serializable
    @SerialName("AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER")
    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
        val prisbetingelser: String?,
        val satser: List<AvtaltSatsDto>,
    ) : PrismodellDto() {
        @Transient
        override val type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
    }
}
