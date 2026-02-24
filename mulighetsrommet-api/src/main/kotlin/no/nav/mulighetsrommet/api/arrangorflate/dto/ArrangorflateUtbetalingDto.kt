package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarselDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorAvbrytStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
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
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrangorflateUtbetalingStatus,
    @Serializable(with = LocalDateSerializer::class)
    val innsendtAvArrangorDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val utbetalesTidligstDato: LocalDate?,
    val kanViseBeregning: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
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
    val kanAvbrytes: ArrangorAvbrytStatus,
    @Serializable(with = LocalDateSerializer::class)
    val avbruttDato: LocalDate?,
    val kanRegenereres: Boolean,
    @Serializable(with = UUIDSerializer::class)
    val regenerertId: UUID?,
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
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: ArrangorflateTilsagnSummary,
    val status: DelutbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val statusSistOppdatert: LocalDateTime?,
    val pris: ValutaBelop,
)
