package no.nav.mulighetsrommet.api.domain.deltaker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class DeltakerForslag(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val endring: Endring,
    val status: Status,
) {
    enum class Status {
        GODKJENT,
        AVVIST,
        TILBAKEKALT,
        ERSTATTET,
        VENTER_PA_SVAR,
    }

    @Serializable
    sealed interface Endring {
        @Serializable
        @SerialName("ForlengDeltakelse")
        data class ForlengDeltakelse(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate,
        ) : Endring

        @Serializable
        @SerialName("AvsluttDeltakelse")
        data class AvsluttDeltakelse(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate? = null,
            val aarsak: EndringAarsak?,
            val harDeltatt: Boolean? = null,
            val harFullfort: Boolean? = null,
        ) : Endring

        @Serializable
        @SerialName("IkkeAktuell")
        data class IkkeAktuell(
            val aarsak: EndringAarsak,
        ) : Endring

        @Serializable
        @SerialName("Deltakelsesmengde")
        data class Deltakelsesmengde(
            val deltakelsesprosent: Int,
            val dagerPerUke: Int? = null,
            @Serializable(with = LocalDateSerializer::class)
            val gyldigFra: LocalDate? = null,
        ) : Endring

        @Serializable
        @SerialName("Startdato")
        data class Startdato(
            @Serializable(with = LocalDateSerializer::class)
            val startdato: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate? = null,
        ) : Endring

        @Serializable
        @SerialName("Sluttdato")
        data class Sluttdato(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate,
        ) : Endring

        @Serializable
        @SerialName("Sluttarsak")
        data class Sluttarsak(
            val aarsak: EndringAarsak,
        ) : Endring

        @Serializable
        @SerialName("FjernOppstartsdato")
        data object FjernOppstartsdato : Endring

        @Serializable
        @SerialName("EndreAvslutning")
        data class EndreAvslutning(
            val aarsak: EndringAarsak?,
            val harDeltatt: Boolean?,
            val harFullfort: Boolean?,
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate? = null,
        ) : Endring
    }

    @Serializable
    sealed interface EndringAarsak {
        @Serializable
        @SerialName("Syk")
        data object Syk : EndringAarsak

        @Serializable
        @SerialName("FattJobb")
        data object FattJobb : EndringAarsak

        @Serializable
        @SerialName("TrengerAnnenStotte")
        data object TrengerAnnenStotte : EndringAarsak

        @Serializable
        @SerialName("Utdanning")
        data object Utdanning : EndringAarsak

        @Serializable
        @SerialName("IkkeMott")
        data object IkkeMott : EndringAarsak

        @Serializable
        @SerialName("Annet")
        data class Annet(
            val beskrivelse: String,
        ) : EndringAarsak
    }
}
