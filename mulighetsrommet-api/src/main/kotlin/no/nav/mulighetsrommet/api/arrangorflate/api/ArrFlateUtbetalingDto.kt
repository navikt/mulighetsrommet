package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrFlateUtbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrFlateUtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    val tiltakstype: Utbetaling.Tiltakstype,
    val gjennomforing: Utbetaling.Gjennomforing,
    val arrangor: Utbetaling.Arrangor,
    val beregning: Beregning,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val periode: Periode,
    val linjer: List<ArrangorUtbetalingLinje>,
)

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
        val stengt: List<StengtPeriode>,
    ) : Beregning()

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val digest: String,
    ) : Beregning()
}

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
    val perioder: List<DeltakelsePeriode>,
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
