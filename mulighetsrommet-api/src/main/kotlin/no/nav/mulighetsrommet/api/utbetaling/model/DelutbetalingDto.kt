package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
sealed class DelutbetalingDto {
    abstract val tilsagnId: UUID
    abstract val utbetalingId: UUID
    abstract val belop: Int
    abstract val periode: Periode
    abstract val opprettetAv: NavIdent

    @Serializable
    @SerialName("DELUTBETALING_TIL_GODKJENNIG")
    data class DelutbetalingTilGodkjenning(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val opprettetAv: NavIdent,
    ) : DelutbetalingDto()

    @Serializable
    @SerialName("DELUTBETALING_GODKJENT")
    data class DelutbetalingGodkjent(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val opprettetAv: NavIdent,
        val besluttetAv: NavIdent,
    ) : DelutbetalingDto()
}
