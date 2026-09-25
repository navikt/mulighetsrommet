package no.nav.mulighetsrommet.admin.totrinnskontroll

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class TotrinnskontrollDto {
    abstract val id: UUID
    abstract val behandletAv: AgentDto
    abstract val behandletTidspunkt: LocalDateTime
    abstract val behandletBegrunnelse: String?
    abstract val behandletAarsaker: List<String>

    @Serializable
    @SerialName("TotrinnskontrollDto.TilBeslutning")
    data class TilBeslutning(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val behandletBegrunnelse: String?,
        override val behandletAarsaker: List<String>,
    ) : TotrinnskontrollDto()

    @Serializable
    @SerialName("TotrinnskontrollDto.Besluttet")
    data class Besluttet(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val behandletBegrunnelse: String?,
        override val behandletAarsaker: List<String>,
        val besluttetAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val besluttetBegrunnelse: String?,
        val besluttetAarsaker: List<String>,
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
    val navn: String?,
) {
    companion object {
        fun fromAgent(agent: Agent, navAnsattNavn: String?) = when (agent) {
            is Arrangor -> AgentDto(agent, "Arrangør")
            is Tiltaksadministrasjon -> AgentDto(agent, "Tiltaksadministrasjon")
            is Arena -> AgentDto(agent, "Arena")
            is NavIdent -> AgentDto(agent, navAnsattNavn)
        }
    }
}
