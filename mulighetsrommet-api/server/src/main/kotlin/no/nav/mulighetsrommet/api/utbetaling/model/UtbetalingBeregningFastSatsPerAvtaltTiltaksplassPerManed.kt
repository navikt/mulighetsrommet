package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

@Serializable
data class UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class TilsagnInput(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        val periode: Periode,
        val beregnetBelop: ValutaBelop,
        val gjenstaendeBelop: ValutaBelop,
    )

    @Serializable
    data class TilsagnBidrag(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        val periode: Periode,
        val bidrag: ValutaBelop,
    )

    @Serializable
    data class Input(
        val tilsagn: List<TilsagnInput>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser(): Set<UtbetalingBeregningInputDeltakelse> = emptySet()
    }

    @Serializable
    data class Output(
        override val pris: ValutaBelop,
        val tilsagnBidrag: List<TilsagnBidrag>,
    ) : UtbetalingBeregningOutput() {
        override fun deltakelser(): Set<UtbetalingBeregningOutputDeltakelse> = emptySet()
    }

    override fun deltakelsePerioder(): Set<DeltakelsePeriode> = emptySet()
}

/**
 * Kompleksiteten i beregningen ligger i at utbetalingen er av typen "avtalt pris per tiltaksplass per måned", men
 * utleder dette fra tilsagnet som ikke har annen tilgjengelig data enn periode og beløp (i hvert fall i
 * utgangspunkntet - systemet krever ikke at man har beregnet tilsagnet på en spesifikk måte for å kunne benytte det
 * til en hvilken som helst type utbetaling).
 * Vi må derfor regne oss "tilbake" til hvilken verdi hver månedsperiode  i tilsagnet har basert på totalbeløpet i
 * tilsagnet og deretter den delen av tilsagnsbeløpet som tilsvarer perioden i utbetalingen.
 * Dette kunne i praksis ført til små avrundingsfeil om vi hadde støttet utbetalingsperioder på noe annet enn hele
 * måneder, men siden det er systemet som oppretter utbetalingene kan vi sikre at periodene alltid er hele måneder,
 * slik at vi kan sikre at hvert månedsbeløp fra tilsagnet alltid blir benyttet.
 */
object FastSatsPerAvtaltTiltaksplassPerManedBeregning :
    SystemgenerertPrismodell.FraTilsagn<UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed> {

    override val type = PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        tilsagn: List<Tilsagn>,
    ): UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed {
        val valuta = gjennomforing.prismodell.valuta
        val tilsagnInput = tilsagn
            .filter { it.beregning.output.pris.valuta == valuta && it.periode.intersects(periode) }
            .map {
                UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnInput(
                    tilsagnId = it.id,
                    periode = it.periode,
                    beregnetBelop = it.beregning.output.pris,
                    gjenstaendeBelop = it.gjenstaendeBelop(),
                )
            }
        val tilsagnBidrag = tilsagnInput.mapNotNull {
            val overlap = it.periode.intersect(periode) ?: return@mapNotNull null
            val amountByMonth = UtbetalingBeregningHelpers.distributeAmountByMonthsInPeriode(
                it.beregnetBelop.belop,
                it.periode,
            )
            val bidrag = minOf(
                UtbetalingBeregningHelpers.amountForOverlap(overlap, amountByMonth),
                it.gjenstaendeBelop.belop,
            )
            UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnBidrag(
                tilsagnId = it.tilsagnId,
                periode = overlap,
                bidrag = ValutaBelop(bidrag, valuta),
            )
        }
        val totalBelop = tilsagnBidrag.sumOf { it.bidrag.belop }
        return UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed(
            input = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Input(
                tilsagn = tilsagnInput,
            ),
            output = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Output(
                pris = ValutaBelop(totalBelop, valuta),
                tilsagnBidrag = tilsagnBidrag,
            ),
        )
    }
}
