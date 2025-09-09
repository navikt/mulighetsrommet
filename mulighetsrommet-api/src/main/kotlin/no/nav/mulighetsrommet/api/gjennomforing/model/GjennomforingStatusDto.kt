package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class GjennomforingStatusDto {
    abstract val type: GjennomforingStatus

    @Serializable
    @SerialName("GJENNOMFORES")
    data object Gjennomfores : GjennomforingStatusDto() {
        @Transient
        override val type = GjennomforingStatus.GJENNOMFORES
    }

    @Serializable
    @SerialName("AVSLUTTET")
    data object Avsluttet : GjennomforingStatusDto() {
        @Transient
        override val type = GjennomforingStatus.AVSLUTTET
    }

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingStatusDto() {
        @Transient
        override val type = GjennomforingStatus.AVBRUTT
    }

    @Serializable
    @SerialName("AVLYST")
    data class Avlyst(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingStatusDto() {
        @Transient
        override val type = GjennomforingStatus.AVLYST
    }
}
