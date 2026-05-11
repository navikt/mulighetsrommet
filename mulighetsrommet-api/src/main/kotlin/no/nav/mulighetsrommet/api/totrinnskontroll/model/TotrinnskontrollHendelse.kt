package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.serializers.AgentSerializer
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
    val type: Totrinnskontroll.Type,
    @Serializable(with = AgentSerializer::class)
    val behandletAv: Agent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    @Serializable(with = AgentSerializer::class)
    val besluttetAv: Agent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val besluttelse: Besluttelse?,
    val aarsaker: List<String>,
    val forklaring: String?,
)
