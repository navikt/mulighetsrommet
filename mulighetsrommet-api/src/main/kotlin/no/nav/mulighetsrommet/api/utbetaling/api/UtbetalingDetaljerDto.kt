package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val handlinger: Set<UtbetalingHandling>,
)

@Serializable
enum class UtbetalingHandling {
    OPPRETT_KORREKSJON,
    REDIGER,
    SEND_TIL_ATTESTERING,
    SLETT,
}

@Serializable
data class UtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: UtbetalingLinjeStatusDto?,
    val pris: ValutaBelop,
    val gjorOppTilsagn: Boolean,
    val opprettelse: TotrinnskontrollDto?,
    val handlinger: Set<UtbetalingLinjeHandling>,
)

@Serializable
enum class UtbetalingLinjeHandling {
    ATTESTER,
    RETURNER,
    SEND_TIL_ATTESTERING,
}

@Serializable
data class UtbetalingLinjeStatusDto(
    val type: UtbetalingLinjeStatus,
    val status: DataElement.Status,
) {
    companion object {
        fun fromUtbetalingLinjeStatus(status: UtbetalingLinjeStatus): UtbetalingLinjeStatusDto {
            return UtbetalingLinjeStatusDto(
                type = status,
                status = DataElement.Status(
                    value = status.beskrivelse,
                    when (status) {
                        UtbetalingLinjeStatus.GODKJENT,
                        UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
                        UtbetalingLinjeStatus.UTBETALT,
                        -> DataElement.Status.Variant.SUCCESS

                        UtbetalingLinjeStatus.TIL_ATTESTERING,
                        -> DataElement.Status.Variant.INFO

                        UtbetalingLinjeStatus.RETURNERT,
                        -> DataElement.Status.Variant.ERROR
                    },
                ),
            )
        }
    }
}
