package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.model.GjennomforingStatusType

/**
 * Status for gjennomføringer som er knyttet til en avtale, eller som fortsatt forvaltes i Arena.
 */
@Serializable
sealed class GjennomforingAvtaleStatus {
    abstract val type: GjennomforingStatusType

    @Serializable
    @SerialName("GJENNOMFORES")
    data object Gjennomfores : GjennomforingAvtaleStatus() {
        @Transient
        override val type = GjennomforingStatusType.GJENNOMFORES
    }

    @Serializable
    @SerialName("AVSLUTTET")
    data object Avsluttet : GjennomforingAvtaleStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVSLUTTET
    }

    @Serializable
    @SerialName("AVLYST")
    data class Avlyst(
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingAvtaleStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVLYST
    }

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    ) : GjennomforingAvtaleStatus() {
        @Transient
        override val type = GjennomforingStatusType.AVBRUTT
    }

    companion object {
        fun from(
            type: GjennomforingStatusType,
            aarsaker: List<AvbrytGjennomforingAarsak>,
            forklaring: String?,
        ): GjennomforingAvtaleStatus = when (type) {
            GjennomforingStatusType.GJENNOMFORES -> Gjennomfores

            GjennomforingStatusType.AVSLUTTET -> Avsluttet

            GjennomforingStatusType.AVLYST -> Avlyst(aarsaker, forklaring)

            GjennomforingStatusType.AVBRUTT -> Avbrutt(aarsaker, forklaring)

            GjennomforingStatusType.ENKELTPLASS_UTKAST_TIL_PAMELDING,
            GjennomforingStatusType.ENKELTPLASS_SOKT_INN,
            GjennomforingStatusType.ENKELTPLASS_VENTER_PA_OPPSTART,
            GjennomforingStatusType.ENKELTPLASS_DELTAR,
            GjennomforingStatusType.ENKELTPLASS_IKKE_AKTUELL,
            GjennomforingStatusType.ENKELTPLASS_FULLFORT,
            GjennomforingStatusType.ENKELTPLASS_AVBRUTT,
            GjennomforingStatusType.ENKELTPLASS_AVBRUTT_UTKAST,
            GjennomforingStatusType.ENKELTPLASS_FEILREGISTRERT,
            -> error("$type er kun støttet for gjennomføringer av type enkeltplass")
        }
    }
}

/**
 * Status for enkeltplasser. Statusen speiler statusen til deltakeren som er knyttet til gjennomføringen.
 */
@Serializable
sealed class GjennomforingEnkeltplassStatus {
    abstract val type: GjennomforingStatusType

    @Serializable
    @SerialName("ENKELTPLASS_UTKAST_TIL_PAMELDING")
    data object UtkastTilPamelding : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_UTKAST_TIL_PAMELDING
    }

    @Serializable
    @SerialName("ENKELTPLASS_SOKT_INN")
    data object SoktInn : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_SOKT_INN
    }

    @Serializable
    @SerialName("ENKELTPLASS_VENTER_PA_OPPSTART")
    data object VenterPaOppstart : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_VENTER_PA_OPPSTART
    }

    @Serializable
    @SerialName("ENKELTPLASS_DELTAR")
    data object Deltar : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_DELTAR
    }

    @Serializable
    @SerialName("ENKELTPLASS_IKKE_AKTUELL")
    data object IkkeAktuell : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_IKKE_AKTUELL
    }

    @Serializable
    @SerialName("ENKELTPLASS_FULLFORT")
    data object Fullfort : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_FULLFORT
    }

    @Serializable
    @SerialName("ENKELTPLASS_AVBRUTT")
    data object Avbrutt : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_AVBRUTT
    }

    @Serializable
    @SerialName("ENKELTPLASS_AVBRUTT_UTKAST")
    data object AvbruttUtkast : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_AVBRUTT_UTKAST
    }

    @Serializable
    @SerialName("ENKELTPLASS_FEILREGISTRERT")
    data object Feilregistrert : GjennomforingEnkeltplassStatus() {
        @Transient
        override val type = GjennomforingStatusType.ENKELTPLASS_FEILREGISTRERT
    }

    companion object {
        fun from(type: GjennomforingStatusType): GjennomforingEnkeltplassStatus = when (type) {
            GjennomforingStatusType.ENKELTPLASS_UTKAST_TIL_PAMELDING -> UtkastTilPamelding

            GjennomforingStatusType.ENKELTPLASS_SOKT_INN -> SoktInn

            GjennomforingStatusType.ENKELTPLASS_VENTER_PA_OPPSTART -> VenterPaOppstart

            GjennomforingStatusType.ENKELTPLASS_DELTAR -> Deltar

            GjennomforingStatusType.ENKELTPLASS_IKKE_AKTUELL -> IkkeAktuell

            GjennomforingStatusType.ENKELTPLASS_FULLFORT -> Fullfort

            GjennomforingStatusType.ENKELTPLASS_AVBRUTT -> Avbrutt

            GjennomforingStatusType.ENKELTPLASS_AVBRUTT_UTKAST -> AvbruttUtkast

            GjennomforingStatusType.ENKELTPLASS_FEILREGISTRERT -> Feilregistrert

            GjennomforingStatusType.GJENNOMFORES,
            GjennomforingStatusType.AVSLUTTET,
            GjennomforingStatusType.AVBRUTT,
            GjennomforingStatusType.AVLYST,
            -> error("$type er ikke støttet for gjennomføringer av type enkeltplass")
        }
    }
}
