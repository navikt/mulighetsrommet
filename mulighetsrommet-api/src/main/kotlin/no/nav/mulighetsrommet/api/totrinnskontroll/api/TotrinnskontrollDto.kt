package no.nav.mulighetsrommet.api.totrinnskontroll.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class TotrinnskontrollDto {
    abstract val behandletAv: AgentDto
    abstract val behandletTidspunkt: LocalDateTime
    abstract val aarsaker: List<String>
    abstract val forklaring: String?

    @Serializable
    data class TilBeslutning(
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
    ) : TotrinnskontrollDto()

    @Serializable
    data class Besluttet(
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        val besluttetAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val beslutning: Beslutning,
    ) : TotrinnskontrollDto()

    enum class Beslutning {
        SATT_PA_VENT,
        GODKJENT,
        RETURNERT,
    }
}

@Serializable
data class AgentDto(
    @Serializable(with = AgentSerializer::class)
    val agent: Agent,
    val navn: String,
) {
    companion object {
        fun fromAgent(agent: Agent, navAnsattNavn: String?) = when (agent) {
            is Arrangor -> AgentDto(agent, "Arrangør")
            is Tiltaksadministrasjon -> AgentDto(agent, "Tiltaksadministrasjon")
            is Arena -> AgentDto(agent, "Arena")
            is NavIdent -> AgentDto(agent, navAnsattNavn ?: agent.value)
        }
    }
}
