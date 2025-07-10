package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
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
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    val tiltakstype: Utbetaling.Tiltakstype,
    val gjennomforing: Utbetaling.Gjennomforing,
    val arrangor: Utbetaling.Arrangor,
    val beregning: ArrFlateBeregning,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val periode: Periode,
    val type: UtbetalingType?,
    val linjer: List<ArrangorUtbetalingLinje>,
)

@Serializable
sealed class ArrFlateBeregning {
    abstract val belop: Int
    abstract val digest: String

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER")
    data class PrisPerManedsverkMedDeltakelsesmengder(
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<UtbetalingDeltakelseManedsverk>,
        val stengt: List<StengtPeriode>,
        val antallManedsverk: Double,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<UtbetalingDeltakelseManedsverk2>,
        val stengt: List<StengtPeriode>,
        val antallManedsverk: Double,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<UtbetalingDeltakelseUkesverk>,
        val stengt: List<StengtPeriode>,
        val antallUkesverk: Double,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val digest: String,
    ) : ArrFlateBeregning()
}

@Serializable
data class UtbetalingDeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forstePeriodeStartDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sistePeriodeSluttDato: LocalDate,
    val sistePeriodeDeltakelsesprosent: Double,
    val perioder: List<DeltakelsesprosentPeriode>,
    val manedsverk: Double,
    val person: UtbetalingDeltakelsePerson?,
)

@Serializable
data class UtbetalingDeltakelseManedsverk2(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val deltakerStartDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStartDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSluttDato: LocalDate,
    val manedsverk: Double,
    val person: UtbetalingDeltakelsePerson?,
)

@Serializable
data class UtbetalingDeltakelseUkesverk(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val deltakerStartDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStartDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSluttDato: LocalDate,
    val ukesverk: Double,
    val person: UtbetalingDeltakelsePerson?,
)

@Serializable
data class UtbetalingDeltakelsePerson(
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val fodselsdato: LocalDate?,
    val fodselsaar: Int?,
)
