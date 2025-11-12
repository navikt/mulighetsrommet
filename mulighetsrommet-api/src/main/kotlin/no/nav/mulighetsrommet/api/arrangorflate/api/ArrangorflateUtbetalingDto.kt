package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrangorflateUtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrangorflateUtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    val kanViseBeregning: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    val tiltakstype: ArrangorflateTiltakstype,
    val gjennomforing: ArrangorflateGjennomforingInfo,
    val arrangor: ArrangorflateArrangor,
    val betalingsinformasjon: ArrangorflateBetalingsinformasjon,
    val beregning: ArrangorflateBeregning,
    val periode: Periode,
    val type: UtbetalingTypeDto,
    val linjer: List<ArrangforflateUtbetalingLinje>,
    val advarsler: List<DeltakerAdvarsel>,
)

@Serializable
data class ArrangorflateBetalingsinformasjon(
    val kontonummer: Kontonummer?,
    val kid: Kid?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class ArrangorflateBeregning {
    abstract val displayName: String
    abstract val detaljer: Details
    abstract val belop: Int
    abstract val digest: String

    @Serializable
    @SerialName("ArrangorflateBeregningFastSatsPerTiltaksplassPerManed")
    data class FastSatsPerTiltaksplassPerManed(
        override val belop: Int,
        override val digest: String,
        override val detaljer: Details,
        val deltakelser: List<ArrangorflateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
    ) : ArrangorflateBeregning() {
        override val displayName: String = "Fast sats per tiltaksplass per måned"
    }

    @Serializable
    @SerialName("ArrangorflateBeregningPrisPerManedsverk")
    data class PrisPerManedsverk(
        override val belop: Int,
        override val digest: String,
        override val detaljer: Details,
        val deltakelser: List<ArrangorflateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
    ) : ArrangorflateBeregning() {
        override val displayName: String = "Avtalt månedspris per tiltaksplass"
    }

    @Serializable
    @SerialName("ArrangorflateBeregningPrisPerUkesverk")
    data class PrisPerUkesverk(
        override val belop: Int,
        override val digest: String,
        override val detaljer: Details,
        val deltakelser: List<ArrangorflateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
    ) : ArrangorflateBeregning() {
        override val displayName: String = "Avtalt ukespris per tiltaksplass"
    }

    @Serializable
    @SerialName("ArrangorflateBeregningPrisPerTimeOppfolging")
    data class PrisPerTimeOppfolging(
        override val belop: Int,
        override val digest: String,
        override val detaljer: Details,
        val deltakelser: List<ArrangorflateBeregningDeltakelse>,
        val stengt: List<StengtPeriode>,
    ) : ArrangorflateBeregning() {
        override val displayName: String = "Avtalt pris per time oppfølging per deltaker"
    }

    @Serializable
    @SerialName("ArrangorflateBeregningFri")
    data class Fri(
        override val belop: Int,
        override val digest: String,
        override val detaljer: Details,
    ) : ArrangorflateBeregning() {
        override val displayName: String = "Annen avtalt pris"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class ArrangorflateBeregningDeltakelse {
    abstract val id: UUID
    abstract val deltakerStartDato: LocalDate?
    abstract val periode: Periode
    abstract val personalia: ArrangorflatePersonalia?
    abstract val status: DeltakerStatusType?

    @Serializable
    @SerialName("ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed")
    data class FastSatsPerTiltaksplassPerManed(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        val faktor: Double,
        val perioderMedDeltakelsesmengde: List<DeltakelsesprosentPeriode>,
        override val periode: Periode,
        override val personalia: ArrangorflatePersonalia?,
        override val status: DeltakerStatusType?,
    ) : ArrangorflateBeregningDeltakelse()

    @Serializable
    @SerialName("ArrangorflateBeregningDeltakelsePrisPerManedsverk")
    data class PrisPerManedsverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        val faktor: Double,
        override val periode: Periode,
        override val personalia: ArrangorflatePersonalia?,
        override val status: DeltakerStatusType?,
    ) : ArrangorflateBeregningDeltakelse()

    @Serializable
    @SerialName("ArrangorflateBeregningDeltakelsePrisPerUkesverk")
    data class PrisPerUkesverk(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        val faktor: Double,
        override val periode: Periode,
        override val personalia: ArrangorflatePersonalia?,
        override val status: DeltakerStatusType?,
    ) : ArrangorflateBeregningDeltakelse()

    @Serializable
    @SerialName("ArrangorflateBeregningDeltakelsePrisPerTimeOppfolging")
    data class PrisPerTimeOppfolging(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val deltakerStartDato: LocalDate?,
        override val periode: Periode,
        override val personalia: ArrangorflatePersonalia?,
        override val status: DeltakerStatusType?,
    ) : ArrangorflateBeregningDeltakelse()
}

@Serializable
data class ArrangorflatePersonalia(
    val navn: String,
    val norskIdent: NorskIdent?,
    val erSkjermet: Boolean,
) {
    companion object {
        fun fromPersonalia(personalia: DeltakerPersonalia) = when (personalia.adressebeskyttelse) {
            PdlGradering.UGRADERT -> {
                ArrangorflatePersonalia(
                    navn = personalia.navn,
                    norskIdent = personalia.norskIdent,
                    erSkjermet = personalia.erSkjermet,
                )
            }

            else -> ArrangorflatePersonalia(
                navn = "Adressebeskyttet",
                norskIdent = null,
                erSkjermet = personalia.erSkjermet,
            )
        }
    }
}

@Serializable
data class ArrangforflateUtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: ArrangorflateTilsagnSummary,
    val status: DelutbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val statusSistOppdatert: LocalDateTime?,
    val belop: Int,
)
