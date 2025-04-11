package no.nav.mulighetsrommet.api.totrinnskontroll.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class TotrinnskontrollDto {
    abstract val behandletAv: AgentDto
    abstract val behandletTidspunkt: LocalDateTime
    abstract val aarsaker: List<String>
    abstract val forklaring: String?
    abstract val kanBesluttes: Boolean

    @Serializable
    @SerialName("TIL_BESLUTNING")
    data class TilBeslutning(
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        override val kanBesluttes: Boolean,
    ) : TotrinnskontrollDto()

    @Serializable
    @SerialName("BESLUTTET")
    data class Besluttet(
        override val behandletAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        val besluttetAv: AgentDto,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val besluttelse: Besluttelse,
    ) : TotrinnskontrollDto() {
        override val kanBesluttes = false
    }

    companion object {
        fun fromTotrinnskontroll(
            totrinnskontroll: Totrinnskontroll,
            kanBesluttes: Boolean,
            behandletAvNavn: String?,
            besluttetAvNavn: String?,
        ) = when {
            totrinnskontroll.besluttetAv == null -> TilBeslutning(
                behandletAv = toAgentDto(totrinnskontroll.behandletAv, behandletAvNavn),
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                kanBesluttes = kanBesluttes,
            )

            else -> Besluttet(
                behandletAv = toAgentDto(totrinnskontroll.behandletAv, behandletAvNavn),
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                besluttetAv = toAgentDto(totrinnskontroll.besluttetAv, besluttetAvNavn),
                besluttetTidspunkt = checkNotNull(totrinnskontroll.besluttetTidspunkt),
                besluttelse = checkNotNull(totrinnskontroll.besluttelse),
            )
        }
    }
}

fun toAgentDto(agent: Agent, navAnsattNavn: String?) = when (agent) {
    is Arrangor -> AgentDto.Arrangor
    is Tiltaksadministrasjon -> AgentDto.System("Tiltaksadministrasjon")
    is Arena -> AgentDto.System("Arena")
    is NavIdent -> AgentDto.NavAnsatt(agent, navAnsattNavn)
}

@Serializable
sealed class AgentDto {
    @Serializable
    @SerialName("NAV_ANSATT")
    data class NavAnsatt(
        val navIdent: NavIdent,
        val navn: String?,
    ) : AgentDto()

    @Serializable
    @SerialName("ARRANGOR")
    data object Arrangor : AgentDto()

    @Serializable
    @SerialName("SYSTEM")
    data class System(
        val navn: String,
    ) : AgentDto()
}
