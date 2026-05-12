package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
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
    val behandletAv: TotrinnskontrollAgent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val besluttetAv: TotrinnskontrollAgent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val besluttelse: TotrinnskontrollBesluttelse?,
    val aarsaker: List<String>,
    val forklaring: String?,
)
