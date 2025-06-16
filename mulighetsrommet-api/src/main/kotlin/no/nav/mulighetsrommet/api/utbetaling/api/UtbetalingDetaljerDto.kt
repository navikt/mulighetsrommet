package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val deltakere: List<DeltakerForKostnadsfordeling>,
    val linjer: List<UtbetalingLinje>,
)

@Serializable
data class UtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: DelutbetalingStatus,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
    val opprettelse: TotrinnskontrollDto,
)

@Serializable
data class ArrangorUtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: DelutbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val statusSistOppdatert: LocalDateTime?,
    val belop: Int,
)

fun toReadableName(delutbetalingStatus: DelutbetalingStatus): String {
    return when (delutbetalingStatus) {
        DelutbetalingStatus.TIL_ATTESTERING -> "Til godkjenning"
        DelutbetalingStatus.GODKJENT -> "Godkjent"
        DelutbetalingStatus.RETURNERT -> "Returnert"
        DelutbetalingStatus.UTBETALT -> "Utbetalt"
        DelutbetalingStatus.OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
        DelutbetalingStatus.BEHANDLES_AV_NAV -> "Behandles av NAV"
    }
}

@Serializable
data class DeltakerForKostnadsfordeling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String?,
    val geografiskEnhet: NavEnhetDbo?,
    val region: NavEnhetDbo?,
    @Serializable(with = LocalDateSerializer::class)
    val foedselsdato: LocalDate?,
    val manedsverk: Double,
)
