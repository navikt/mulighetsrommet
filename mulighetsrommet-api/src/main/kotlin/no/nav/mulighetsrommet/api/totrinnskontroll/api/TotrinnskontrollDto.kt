package no.nav.mulighetsrommet.api.totrinnskontroll.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.TilBeslutning
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
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
        val kanBesluttes: Boolean,
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
        val besluttelse: Besluttelse,
    ) : TotrinnskontrollDto()
}

fun Totrinnskontroll.toDto(
    kanBesluttes: Boolean,
) = when {
    besluttetAv == null -> TilBeslutning(
        behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
        behandletTidspunkt = behandletTidspunkt,
        aarsaker = aarsaker,
        forklaring = forklaring,
        kanBesluttes = kanBesluttes,
    )

    else -> Besluttet(
        behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
        behandletTidspunkt = behandletTidspunkt,
        aarsaker = aarsaker,
        forklaring = forklaring,
        besluttetAv = AgentDto.fromAgent(besluttetAv, besluttetAvNavn),
        besluttetTidspunkt = checkNotNull(besluttetTidspunkt),
        besluttelse = checkNotNull(besluttelse),
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class AgentDto {
    @Serializable
    data class NavAnsatt(
        val navIdent: NavIdent,
        val navn: String?,
    ) : AgentDto()

    @Serializable
    data object Arrangor : AgentDto()

    @Serializable
    data class System(
        val navn: String,
    ) : AgentDto()

    companion object {
        fun fromAgent(agent: Agent, navAnsattNavn: String?) = when (agent) {
            is no.nav.mulighetsrommet.model.Arrangor -> Arrangor
            is Tiltaksadministrasjon -> System("Tiltaksadministrasjon")
            is Arena -> System("Arena")
            is NavIdent -> NavAnsatt(agent, navAnsattNavn)
        }
    }
}
