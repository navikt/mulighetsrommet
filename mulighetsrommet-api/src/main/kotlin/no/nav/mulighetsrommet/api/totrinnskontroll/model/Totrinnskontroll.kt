package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.util.UUID

@Serializable
data class Totrinnskontroll(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    @Serializable(with = AgentSerializer::class)
    val behandletAv: Agent,
    val behandletAvNavn: String?,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val aarsaker: List<String>,
    val forklaring: String?,
    @Serializable(with = AgentSerializer::class)
    val besluttetAv: Agent?,
    val besluttetAvNavn: String?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val besluttelse: TotrinnskontrollBesluttelse?,
)

enum class TotrinnskontrollType {
    TILSAGN_OPPRETTELSE,
    TILSAGN_ANNULLERING,
    TILSAGN_OPPGJOR,
    UTBETALING_LINJE_OPPRETTELSE,
    ENKELTPLASS_OKONOMI,
    TILSKUDD_OPPRETTELSE,
}

@Serializable
enum class TotrinnskontrollBesluttelse {
    GODKJENT,
    AVVIST,
}
