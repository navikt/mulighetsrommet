package no.nav.mulighetsrommet.domain.dto.amt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed interface Melding {
    val id: UUID
    val deltakerId: UUID
    val opprettetAvArrangorAnsattId: UUID
    val opprettet: LocalDateTime

    @Serializable
    @SerialName("EndringFraArrangor")
    data class EndringFraArrangor(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val opprettetAvArrangorAnsattId: UUID,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettet: LocalDateTime,
        val endring: Endring,
    ) : Melding {
        @Serializable
        sealed interface Endring {
            @Serializable
            @SerialName("LeggTilOppstartsdato")
            data class LeggTilOppstartsdato(
                @Serializable(with = LocalDateSerializer::class)
                val startdato: LocalDate,
                @Serializable(with = LocalDateSerializer::class)
                val sluttdato: LocalDate? = null,
            ) : Endring
        }
    }

    @Serializable
    @SerialName("Forslag")
    data class Forslag(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val opprettetAvArrangorAnsattId: UUID,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettet: LocalDateTime,
        val begrunnelse: String? = null,
        val endring: Endring,
        val status: Status,
    ) : Melding {
        @Serializable
        sealed interface Status {
            @Serializable
            @SerialName("Godkjent")
            data class Godkjent(
                val godkjentAv: NavAnsatt,
                @Serializable(with = LocalDateTimeSerializer::class)
                val godkjent: LocalDateTime,
            ) : Status

            @Serializable
            @SerialName("Avvist")
            data class Avvist(
                val avvistAv: NavAnsatt,
                @Serializable(with = LocalDateTimeSerializer::class)
                val avvist: LocalDateTime,
                val begrunnelseFraNav: String,
            ) : Status

            @Serializable
            @SerialName("Tilbakekalt")
            data class Tilbakekalt(
                @Serializable(with = UUIDSerializer::class)
                val tilbakekaltAvArrangorAnsattId: UUID,
                @Serializable(with = LocalDateTimeSerializer::class)
                val tilbakekalt: LocalDateTime,
            ) : Status

            @Serializable
            @SerialName("Erstattet")
            data class Erstattet(
                @Serializable(with = UUIDSerializer::class)
                val erstattetMedForslagId: UUID,
                @Serializable(with = LocalDateTimeSerializer::class)
                val erstattet: LocalDateTime,
            ) : Status

            @Serializable
            @SerialName("VenterPaSvar")
            data object VenterPaSvar : Status
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
                val aarsak: EndringAarsak,
                val harDeltatt: Boolean? = null,
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
        }

        @Serializable
        data class NavAnsatt(
            @Serializable(with = UUIDSerializer::class)
            val id: UUID,
            @Serializable(with = UUIDSerializer::class)
            val enhetId: UUID,
        )
    }
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
