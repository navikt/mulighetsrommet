package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ArrangorflateUtbetalingRadDto(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val beregnetBelop: ValutaBelop,
    val godkjentBelop: ValutaBelop?,
    val type: String,
    val status: ArrangorflateUtbetalingStatus,
)

fun ArrangorflateUtbetalingKompakt.toRadDto(): ArrangorflateUtbetalingRadDto = ArrangorflateUtbetalingRadDto(
    utbetalingId = id,
    gjennomforing = gjennomforing,
    arrangor = arrangor,
    tiltakstype = tiltakstype,
    startDato = periode.start,
    sluttDato = periode.slutt,
    beregnetBelop = beregnetBelop,
    godkjentBelop = godkjentBelop,
    type = type.displayName,
    status = status,
)
