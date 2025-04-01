package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
    val fnr: String?,
    val status: DeltakerStatus.Type,
    val manedsverk: Double,
) {
    companion object {
        fun List<Deltaker>.toDeltakereForKostnadsfordeling(manedsverkById: Map<UUID, DeltakelseManedsverk>): List<DeltakerForKostnadsfordeling> = this.map {
            DeltakerForKostnadsfordeling(
                id = it.gjennomforingId,
                fnr = it.norskIdent?.value,
                status = it.status.type,
                manedsverk = manedsverkById.getValue(it.id).manedsverk,
            )
        }
    }
}
