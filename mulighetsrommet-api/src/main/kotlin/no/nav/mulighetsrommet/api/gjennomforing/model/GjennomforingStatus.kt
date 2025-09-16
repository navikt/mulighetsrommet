package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class GjennomforingStatus {
    abstract val type: GjennomforingStatusType

    @Serializable
    @SerialName("GJENNOMFORES")
    data object Gjennomfores : GjennomforingStatus() {
        @Transient
        override val type = GjennomforingStatusType.GJENNOMFORES
    }

    @Serializable
    @SerialName("AVSLUTTET")
    data object Avsluttet : GjennomforingStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVSLUTTET
    }

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVBRUTT
    }

    @Serializable
    @SerialName("AVLYST")
    data class Avlyst(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVLYST
    }
}
