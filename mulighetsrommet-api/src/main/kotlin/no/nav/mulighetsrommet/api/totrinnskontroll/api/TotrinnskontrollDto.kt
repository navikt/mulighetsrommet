package no.nav.mulighetsrommet.api.totrinnskontroll.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
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

fun Totrinnskontroll.toDto() = when {
    besluttetAv == null -> TotrinnskontrollDto.TilBeslutning(
        behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
        behandletTidspunkt = behandletTidspunkt.tilNorskLocalDateTime(),
        aarsaker = aarsaker,
        forklaring = forklaring,
    )

    else -> TotrinnskontrollDto.Besluttet(
        behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
        behandletTidspunkt = behandletTidspunkt.tilNorskLocalDateTime(),
        aarsaker = aarsaker,
        forklaring = forklaring,
        besluttetAv = AgentDto.fromAgent(besluttetAv, besluttetAvNavn),
        besluttetTidspunkt = checkNotNull(besluttetTidspunkt).tilNorskLocalDateTime(),
        beslutning = when (status) {
            TotrinnskontrollStatus.TIL_BEHANDLING -> error("Status TIL_BEHANDLING kan ikke mappes til TotrinnskontrollDto.Besluttet")
            TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollDto.Beslutning.SATT_PA_VENT
            TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollDto.Beslutning.GODKJENT
            TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollDto.Beslutning.RETURNERT
        },
    )
}

@Serializable
data class AgentDto(
    val navn: String,
) {
    companion object {
        fun fromAgent(agent: Agent, navAnsattNavn: String?) = when (agent) {
            is Arrangor -> AgentDto("Arrangør")
            is Tiltaksadministrasjon -> AgentDto("Tiltaksadministrasjon")
            is Arena -> AgentDto("Arena")
            is NavIdent -> AgentDto(navAnsattNavn ?: agent.value)
        }
    }
}
