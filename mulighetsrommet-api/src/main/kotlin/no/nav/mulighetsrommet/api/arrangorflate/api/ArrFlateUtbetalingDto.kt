package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.Person
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.DeltakerStatusType
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
    val kanViseBeregning: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    val tiltakstype: ArrangorflateTiltakstype,
    val gjennomforing: ArrangorflateGjennomforingInfo,
    val arrangor: ArrangorflateArrangor,
    val beregning: ArrFlateBeregning,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val periode: Periode,
    val type: UtbetalingType?,
    val linjer: List<ArrangorUtbetalingLinje>,
    val advarsler: List<DeltakerAdvarsel>,
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
        val deltakelser: List<ArrFlateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
        val antallManedsverk: Double,
        val sats: Int,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<ArrFlateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
        val antallManedsverk: Double,
        val sats: Int,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        override val belop: Int,
        override val digest: String,
        val deltakelser: List<ArrFlateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
        val antallUkesverk: Double,
        val sats: Int,
    ) : ArrFlateBeregning()

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val digest: String,
    ) : ArrFlateBeregning()
}

@Serializable
sealed class ArrFlateBeregningDeltakelse {
    abstract val id: UUID
    abstract val deltakerStartDato: LocalDate?
    abstract val periode: Periode
    abstract val person: Person?
    abstract val faktor: Double
    abstract val status: DeltakerStatusType?

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER")
    data class PrisPerManedsverkMedDeltakelsesmengder(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        override val faktor: Double,
        val perioderMedDeltakelsesmengde: List<DeltakelsesprosentPeriode>,
        override val periode: Periode,
        override val person: Person?,
        override val status: DeltakerStatusType?,
    ) : ArrFlateBeregningDeltakelse()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        override val faktor: Double,
        override val periode: Periode,
        override val person: Person?,
        override val status: DeltakerStatusType?,
    ) : ArrFlateBeregningDeltakelse()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        override val faktor: Double,
        override val periode: Periode,
        override val person: Person?,
        override val status: DeltakerStatusType?,
    ) : ArrFlateBeregningDeltakelse()
}
