package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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

@Serializable
class ArrangorflateBeregning(
    val displayName: String,
    val detaljer: Details,
    val belop: Int,
    val digest: String,
    val stengt: List<StengtPeriode>,
    val deltakelser: DataDrivenTableDto?,
)

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
