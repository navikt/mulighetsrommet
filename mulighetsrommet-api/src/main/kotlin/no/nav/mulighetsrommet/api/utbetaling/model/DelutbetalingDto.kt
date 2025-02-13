package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed class DelutbetalingDto {
    abstract val tilsagnId: UUID
    abstract val utbetalingId: UUID
    abstract val belop: Int
    abstract val periode: Periode
    abstract val lopenummer: Int
    abstract val fakturanummer: String
    abstract val opprettetAv: NavIdent
    abstract val opprettetTidspunkt: LocalDateTime

    @Serializable
    @SerialName("DELUTBETALING_TIL_GODKJENNING")
    data class DelutbetalingTilGodkjenning(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val lopenummer: Int,
        override val fakturanummer: String,
        override val opprettetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettetTidspunkt: LocalDateTime,
    ) : DelutbetalingDto()

    @Serializable
    @SerialName("DELUTBETALING_OVERFORT_TIL_UTBETALING")
    data class DelutbetalingOverfortTilUtbetaling(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val lopenummer: Int,
        override val fakturanummer: String,
        override val opprettetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettetTidspunkt: LocalDateTime,
        val besluttetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
    ) : DelutbetalingDto()

    @Serializable
    @SerialName("DELUTBETALING_UTBETALT")
    data class DelutbetalingUtbetalt(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val lopenummer: Int,
        override val fakturanummer: String,
        override val opprettetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettetTidspunkt: LocalDateTime,
        val besluttetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
    ) : DelutbetalingDto()

    @Serializable
    @SerialName("DELUTBETALING_AVVIST")
    data class DelutbetalingAvvist(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        override val utbetalingId: UUID,
        override val belop: Int,
        override val periode: Periode,
        override val lopenummer: Int,
        override val fakturanummer: String,
        override val opprettetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val opprettetTidspunkt: LocalDateTime,
        val besluttetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val aarsaker: List<String>,
        val forklaring: String?,
    ) : DelutbetalingDto()
}
