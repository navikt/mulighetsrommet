package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtbetalingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatusDto,
    val periode: Periode,
    val kostnadssteder: List<KostnadsstedDto>,
    val belopUtbetalt: Int?,
    val type: UtbetalingType,
)
