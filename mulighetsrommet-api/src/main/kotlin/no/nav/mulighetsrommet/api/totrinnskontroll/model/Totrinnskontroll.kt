package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Totrinnskontroll(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: Type,
    @Serializable(with = AgentSerializer::class)
    val behandletAv: Agent,
    val behandletAvNavn: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val aarsaker: List<String>,
    val forklaring: String?,
    @Serializable(with = AgentSerializer::class)
    val besluttetAv: Agent?,
    val besluttetAvNavn: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime?,
    val besluttelse: Besluttelse?,
) {
    enum class Type {
        OPPRETT,
        ANNULLER,
        GJOR_OPP,
    }
}
