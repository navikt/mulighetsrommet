package no.nav.mulighetsrommet.api.contracts.totrinnskontroll

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.util.UUID

@Serializable
data class TotrinnskontrollHendelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val status: Status,
    val behandletAv: TotrinnskontrollAgent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val besluttetAv: TotrinnskontrollAgent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val aarsaker: List<String>,
    val forklaring: String?,
) {
    enum class Status {
        TIL_BEHANDLING,
        SATT_PA_VENT,
        GODKJENT,
        RETURNERT,
    }
}
