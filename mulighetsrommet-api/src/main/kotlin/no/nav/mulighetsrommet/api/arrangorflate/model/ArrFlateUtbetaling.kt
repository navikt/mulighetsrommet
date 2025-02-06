package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed class Beregning {
    abstract val belop: Int
    abstract val digest: String

    @Serializable
    @SerialName("FORHANDSGODKJENT")
    data class Forhandsgodkjent(
        val antallManedsverk: Double,
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<UtbetalingDeltakelse>,
    ) : Beregning()

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val digest: String,
    ) : Beregning()
}

@Serializable
data class ArrFlateUtbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: UtbetalingDto.Tiltakstype,
    val gjennomforing: UtbetalingDto.Gjennomforing,
    val arrangor: UtbetalingDto.Arrangor,
    val beregning: Beregning,
    val betalingsinformasjon: UtbetalingDto.Betalingsinformasjon,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
)

@Serializable
data class UtbetalingDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forstePeriodeStartDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sistePeriodeSluttDato: LocalDate,
    val sistePeriodeDeltakelsesprosent: Double,
    val manedsverk: Double,
    val person: Person?,
    val veileder: String?,
) {
    @Serializable
    data class Person(
        val navn: String,
        @Serializable(with = LocalDateSerializer::class)
        val fodselsdato: LocalDate?,
        val fodselsaar: Int?,
    )
}
