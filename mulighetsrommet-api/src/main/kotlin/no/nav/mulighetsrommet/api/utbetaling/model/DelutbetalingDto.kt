package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
sealed class DelutbetalingDto {
    @Serializable
    @SerialName("DELUTBETALING_TIL_GODKJENNIG")
    data class DelutbetalingTilGodkjenning(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        val utbetalingId: UUID,
        val opprettetAv: NavIdent,
        val belop: Int,
    ) : DelutbetalingDto()

    @Serializable
    @SerialName("DELUTBETALING_GODKJENT")
    data class DelutbetalingGodkjent(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        val utbetalingId: UUID,
        val opprettetAv: NavIdent,
        val besluttetAv: NavIdent,
        val belop: Int,
    ) : DelutbetalingDto()
}
