@file:UseSerializers(UUIDSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarselDto
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ArrangorflateUtbetalingDto(
    val id: UUID,
    val status: ArrangorflateUtbetalingStatus,
    val innsendtAvArrangorDato: LocalDate?,
    val utbetalesTidligstDato: LocalDate?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val betalingsinformasjon: Betalingsinformasjon.BBan?,
    val valuta: Valuta,
    val beregning: ArrangorflateBeregning,
    val periode: Periode,
    val type: UtbetalingTypeDto,
    val innsendingsDetaljer: List<LabeledDataElement>,
    val linjer: List<ArrangforflateUtbetalingLinje>,
    val advarsler: List<DeltakerAdvarselDto>,
    val regenerering: RegenererStatus,
    val avbrytelse: AvbrytStatus,
    val blokkeringer: Set<Utbetaling.Blokkering>,
)

@Serializable
class ArrangorflateBeregning(
    val displayName: String,
    val pris: ValutaBelop,
    val stengt: List<StengtPeriode>,
    val deltakelser: DataDrivenTableDto?,
    val satsDetaljer: List<DataDetails>,
)

@Serializable
data class ArrangforflateUtbetalingLinje(
    val id: UUID,
    val tilsagn: ArrangorflateTilsagnSummary,
    val status: UtbetalingLinjeStatus,
    val statusSistOppdatert: LocalDateTime?,
    val pris: ValutaBelop,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class AvbrytStatus {
    @Serializable
    @SerialName("IKKE_TILGJENGELIG")
    data object IkkeTilgjengelig : AvbrytStatus()

    @Serializable
    @SerialName("KAN_IKKE_AVBRYTES")
    data object KanIkkeAvbrytes : AvbrytStatus()

    @Serializable
    @SerialName("KAN_AVBRYTES")
    data object KanAvbrytes : AvbrytStatus()

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(val detaljer: Avbrytelse) : AvbrytStatus()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class RegenererStatus {
    @Serializable
    @SerialName("IKKE_TILGJENGELIG")
    data object IkkeTilgjengelig : RegenererStatus()

    @Serializable
    @SerialName("KAN_REGENERERES")
    data object KanRegenereres : RegenererStatus()

    @Serializable
    @SerialName("ALLEREDE_REGENERERT")
    data class AlleredeRegenerert(
        val utbetalingId: UUID,
    ) : RegenererStatus()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Avbrytelse {

    @Serializable
    @SerialName("AVBRUTT_AV_ARRANGOR")
    data class Arrangor(
        val avbruttDato: LocalDate,
        val begrunnelse: String?,
    ) : Avbrytelse()

    @Serializable
    @SerialName("AVBRUTT_AV_NAV")
    data class Nav(
        val avbruttDato: LocalDate,
        val aarsaker: List<String>,
        val forklaring: String?,
    ) : Avbrytelse()

    companion object {
        fun fromStatus(
            status: ArrangorflateUtbetalingStatus,
            arrangorAvbrutt: ArrangorflateUtbetaling.ArrangorAvbrutt?,
            avbrytelseTotrinn: TotrinnskontrollDto?,
        ): Avbrytelse? = when (status) {
            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.UTBETALT,
            ArrangorflateUtbetalingStatus.BLOKKERT_FOR_INNSENDING,
            ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
            ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
            -> null

            ArrangorflateUtbetalingStatus.AVBRUTT_AV_ARRANGOR -> arrangorAvbrutt?.let {
                Arrangor(
                    avbruttDato = it.tidspunkt.tilNorskDato(),
                    begrunnelse = it.begrunnelse,
                )
            }
                ?: throw IllegalStateException("Avbrutt tidspunkt må eksistere for status AVBRUTT_AV_ARRANGOR")

            ArrangorflateUtbetalingStatus.AVBRUTT_AV_NAV ->
                (avbrytelseTotrinn as? TotrinnskontrollDto.Besluttet)?.let {
                    Nav(
                        avbruttDato = it.besluttetTidspunkt.toLocalDate(),
                        aarsaker = it.aarsaker,
                        forklaring = it.forklaring,
                    )
                } ?: throw IllegalStateException("Forventet besluttet totrinnskontroll når status er AVBRUTT_AV_NAV")
        }
    }
}
