package no.nav.mulighetsrommet.api.totrinnskontroll.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class TotrinnskontrollDto {
    abstract val behandletAvMetadata: AgentMetadata
    abstract val behandletTidspunkt: LocalDateTime
    abstract val aarsaker: List<String>
    abstract val forklaring: String?
    abstract val kanBesluttes: Boolean

    @Serializable
    @SerialName("TIL_BESLUTNING")
    data class TilBeslutning(
        override val behandletAvMetadata: AgentMetadata,
        val behandletAvNavn: String?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        override val kanBesluttes: Boolean,
    ) : TotrinnskontrollDto()

    @Serializable
    @SerialName("BESLUTTET")
    data class Besluttet(
        override val behandletAvMetadata: AgentMetadata,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        val besluttetAvMetadata: AgentMetadata,
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
                behandletAvMetadata = AgentMetadata(totrinnskontroll.behandletAv, behandletAvNavn),
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                kanBesluttes = kanBesluttes,
                behandletAvNavn = behandletAvNavn,
            )

            else -> Besluttet(
                behandletAvMetadata = AgentMetadata(totrinnskontroll.behandletAv, behandletAvNavn),
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                besluttetAvMetadata = AgentMetadata(totrinnskontroll.besluttetAv, besluttetAvNavn),
                besluttetTidspunkt = checkNotNull(totrinnskontroll.besluttetTidspunkt),
                besluttelse = checkNotNull(totrinnskontroll.besluttelse),
            )
        }
    }
}

@Serializable
data class AgentMetadata(
    @Serializable(with = AgentSerializer::class)
    val type: Agent,
    val navn: String?,
)
