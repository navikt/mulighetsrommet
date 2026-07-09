package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class AvtaleStatus {
    abstract val type: AvtaleStatusType

    @Serializable
    @SerialName("UTKAST")
    data object Utkast : AvtaleStatus() {
        @Transient
        override val type = AvtaleStatusType.UTKAST
    }

    @Serializable
    @SerialName("AKTIV")
    data object Aktiv : AvtaleStatus() {
        @Transient
        override val type = AvtaleStatusType.AKTIV
    }

    @Serializable
    @SerialName("AVSLUTTET")
    data object Avsluttet : AvtaleStatus() {
        @Transient
        override val type = AvtaleStatusType.AVSLUTTET
    }

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbrytAvtaleAarsak>,
        val forklaring: String?,
    ) : AvtaleStatus() {
        @Transient
        override val type = AvtaleStatusType.AVBRUTT
    }
}
