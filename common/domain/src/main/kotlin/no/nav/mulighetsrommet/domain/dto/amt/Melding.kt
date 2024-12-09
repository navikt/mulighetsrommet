package no.nav.mulighetsrommet.domain.dto.amt

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
}

@Serializable
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
        data class LeggTilOppstartsdato(
            @Serializable(with = LocalDateSerializer::class)
            val startdato: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate?,
        ) : Endring
    }
}

@Serializable
data class Forslag(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    override val deltakerId: UUID,
    @Serializable(with = UUIDSerializer::class)
    override val opprettetAvArrangorAnsattId: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    override val opprettet: LocalDateTime,
    val begrunnelse: String?,
    val endring: Endring,
    val status: Status,
) : Melding {
    @Serializable
    sealed interface Status {
        @Serializable
        data class Godkjent(
            val godkjentAv: NavAnsatt,
            @Serializable(with = LocalDateTimeSerializer::class)
            val godkjent: LocalDateTime,
        ) : Status

        @Serializable
        data class Avvist(
            val avvistAv: NavAnsatt,
            @Serializable(with = LocalDateTimeSerializer::class)
            val avvist: LocalDateTime,
            val begrunnelseFraNav: String,
        ) : Status

        @Serializable
        data class Tilbakekalt(
            @Serializable(with = UUIDSerializer::class)
            val tilbakekaltAvArrangorAnsattId: UUID,
            @Serializable(with = LocalDateTimeSerializer::class)
            val tilbakekalt: LocalDateTime,
        ) : Status

        @Serializable
        data class Erstattet(
            @Serializable(with = UUIDSerializer::class)
            val erstattetMedForslagId: UUID,
            @Serializable(with = LocalDateTimeSerializer::class)
            val erstattet: LocalDateTime,
        ) : Status

        @Serializable
        data object VenterPaSvar : Status
    }

    @Serializable
    sealed interface Endring {
        @Serializable
        data class ForlengDeltakelse(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate,
        ) : Endring

        @Serializable
        data class AvsluttDeltakelse(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate?,
            val aarsak: EndringAarsak,
            val harDeltatt: Boolean?,
        ) : Endring

        @Serializable
        data class IkkeAktuell(
            val aarsak: EndringAarsak,
        ) : Endring

        @Serializable
        data class Deltakelsesmengde(
            val deltakelsesprosent: Int,
            val dagerPerUke: Int?,
            @Serializable(with = LocalDateSerializer::class)
            val gyldigFra: LocalDate?,
        ) : Endring

        @Serializable
        data class Startdato(
            @Serializable(with = LocalDateSerializer::class)
            val startdato: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate?,
        ) : Endring

        @Serializable
        data class Sluttdato(
            @Serializable(with = LocalDateSerializer::class)
            val sluttdato: LocalDate,
        ) : Endring

        @Serializable
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

@Serializable
sealed interface EndringAarsak {
    @Serializable
    data object Syk : EndringAarsak

    @Serializable
    data object FattJobb : EndringAarsak

    @Serializable
    data object TrengerAnnenStotte : EndringAarsak

    @Serializable
    data object Utdanning : EndringAarsak

    @Serializable
    data object IkkeMott : EndringAarsak

    @Serializable
    data class Annet(
        val beskrivelse: String,
    ) : EndringAarsak
}
