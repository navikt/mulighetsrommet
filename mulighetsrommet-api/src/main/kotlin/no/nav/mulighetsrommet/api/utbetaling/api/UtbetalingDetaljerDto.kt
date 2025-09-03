package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val linjer: List<UtbetalingLinje>,
    val handlinger: UtbetalingHandlinger,
)

@Serializable
data class UtbetalingHandlinger(
    val sendTilAttestering: Boolean,
)

@Serializable
data class UtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: DelutbetalingStatusDto?,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
    val opprettelse: TotrinnskontrollDto?,
)

@Serializable
data class DelutbetalingStatusDto(
    val type: DelutbetalingStatus,
    val status: DataElement.Status,
) {
    companion object {
        fun fromDelutbetalingStatus(status: DelutbetalingStatus): DelutbetalingStatusDto {
            return DelutbetalingStatusDto(
                type = status,
                status = DataElement.Status(
                    value = status.beskrivelse,
                    when (status) {
                        DelutbetalingStatus.GODKJENT,
                        DelutbetalingStatus.UTBETALT,
                        DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
                        -> DataElement.Status.Variant.SUCCESS

                        DelutbetalingStatus.TIL_ATTESTERING,
                        -> DataElement.Status.Variant.WARNING

                        DelutbetalingStatus.RETURNERT,
                        -> DataElement.Status.Variant.ERROR
                    },
                ),
            )
        }
    }
}
