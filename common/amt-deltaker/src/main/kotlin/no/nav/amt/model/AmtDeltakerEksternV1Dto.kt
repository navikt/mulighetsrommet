package no.nav.amt.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class AmtDeltakerEksternV1Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val personIdent: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: StatusDto,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endretTidspunkt: LocalDateTime,
    val kilde: Kilde,
    val innhold: DeltakelsesinnholdDto?,
    val deltakelsesmengder: List<DeltakelsesmengdeDto>,
) {
    @Serializable
    data class StatusDto(
        val type: DeltakerStatusType,
        val tekst: String,
        val aarsak: AarsakDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        val opprettetTidspunkt: LocalDateTime,
    )

    @Serializable
    data class AarsakDto(
        val type: DeltakerStatusAarsakType?,
        val beskrivelse: String?,
    )

    @Serializable
    data class DeltakelsesinnholdDto(
        val ledetekst: String?,
        val valgtInnhold: List<InnholdDto>,
    )

    @Serializable
    data class InnholdDto(
        val tekst: String,
        val innholdskode: String,
    )

    @Serializable
    data class DeltakelsesmengdeDto(
        val deltakelsesprosent: Float,
        val dagerPerUke: Float?,
        @Serializable(with = LocalDateSerializer::class)
        val gyldigFraDato: LocalDate,
        @Serializable(with = LocalDateTimeSerializer::class)
        val opprettetTidspunkt: LocalDateTime,
    )

    enum class Kilde {
        ARENA,
        KOMET,
    }
}
