package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class AvtaleStatusDto {
    abstract val type: AvtaleStatus

    @Serializable
    @SerialName("UTKAST")
    data object Utkast : AvtaleStatusDto() {
        @Transient
        override val type = AvtaleStatus.UTKAST
    }

    @Serializable
    @SerialName("AKTIV")
    data object Aktiv : AvtaleStatusDto() {
        @Transient
        override val type = AvtaleStatus.AKTIV
    }

    @Serializable
    @SerialName("AVSLUTTET")
    data object Avsluttet : AvtaleStatusDto() {
        @Transient
        override val type = AvtaleStatus.AVSLUTTET
    }

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbruttAarsak>,
        val forklaring: String?,
    ) : AvtaleStatusDto() {
        @Transient
        override val type = AvtaleStatus.AVBRUTT
    }

    companion object {
        fun fromString(name: String, tidspunkt: LocalDateTime?, aarsaker: List<AvbruttAarsak>, forklaring: String?): AvtaleStatusDto = when (AvtaleStatus.valueOf(name)) {
            AvtaleStatus.AKTIV -> Aktiv
            AvtaleStatus.AVSLUTTET -> Avsluttet
            AvtaleStatus.UTKAST -> Utkast
            AvtaleStatus.AVBRUTT -> {
                requireNotNull(tidspunkt)
                Avbrutt(tidspunkt, aarsaker, forklaring)
            }
        }
    }
}
