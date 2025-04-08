package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
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
data class DeltakerForKostnadsfordeling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String?,
    val geografiskEnhet: String?,
    val region: String?,
    @Serializable(with = LocalDateSerializer::class)
    val foedselsdato: LocalDate?,
    val status: DeltakerStatus.Type,
    val manedsverk: Double,
)
