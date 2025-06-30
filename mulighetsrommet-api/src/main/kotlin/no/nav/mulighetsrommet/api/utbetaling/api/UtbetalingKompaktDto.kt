package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtbetalingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: AdminUtbetalingStatus,
    val periode: Periode,
    val kostnadssteder: List<NavEnhetDbo>,
    val belopUtbetalt: Int?,
    val type: UtbetalingType?,
)
