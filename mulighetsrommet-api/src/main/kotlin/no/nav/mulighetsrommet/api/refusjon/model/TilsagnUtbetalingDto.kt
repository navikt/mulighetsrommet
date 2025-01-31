package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
sealed class TilsagnUtbetalingDto {
    @Serializable
    @SerialName("TILSAGN_UTBETALING_TIL_GODKJENNIG")
    data class TilsagnUtbetalingTilGodkjenning(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        val refusjonskravId: UUID,
        val opprettetAv: NavIdent,
        val belop: Int,
    ) : TilsagnUtbetalingDto()

    @Serializable
    @SerialName("TILSAGN_UTBETALING_TIL_GODKJENT")
    data class TilsagnUtbetalingGodkjent(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        val refusjonskravId: UUID,
        val opprettetAv: NavIdent,
        val besluttetAv: NavIdent,
        val belop: Int,
    ) : TilsagnUtbetalingDto()
}
