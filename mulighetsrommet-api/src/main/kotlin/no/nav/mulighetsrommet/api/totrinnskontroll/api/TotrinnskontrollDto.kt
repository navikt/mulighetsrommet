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
    abstract val behandletAv: Agent
    abstract val behandletTidspunkt: LocalDateTime
    abstract val aarsaker: List<String>
    abstract val forklaring: String?
    abstract val kanBesluttes: Boolean

    @Serializable
    @SerialName("TIL_BESLUTNING")
    data class TilBeslutning(
        @Serializable(with = AgentSerializer::class)
        override val behandletAv: Agent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        override val kanBesluttes: Boolean,
    ) : TotrinnskontrollDto()

    @Serializable
    @SerialName("BESLUTTET")
    data class Besluttet(
        @Serializable(with = AgentSerializer::class)
        override val behandletAv: Agent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
        @Serializable(with = AgentSerializer::class)
        val besluttetAv: Agent,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val besluttelse: Besluttelse,
    ) : TotrinnskontrollDto() {
        override val kanBesluttes = false
    }

    companion object {
        fun fromTotrinnskontroll(totrinnskontroll: Totrinnskontroll, kanBesluttes: Boolean) = when {
            totrinnskontroll.besluttetAv == null -> TilBeslutning(
                behandletAv = totrinnskontroll.behandletAv,
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                kanBesluttes = kanBesluttes,
            )

            else -> Besluttet(
                behandletAv = totrinnskontroll.behandletAv,
                behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
                aarsaker = totrinnskontroll.aarsaker,
                forklaring = totrinnskontroll.forklaring,
                besluttetAv = totrinnskontroll.besluttetAv,
                besluttetTidspunkt = checkNotNull(totrinnskontroll.besluttetTidspunkt),
                besluttelse = checkNotNull(totrinnskontroll.besluttelse),
            )
        }
    }
}
