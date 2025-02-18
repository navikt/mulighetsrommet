package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class Totrinnskontroll {
    abstract val behandletAv: NavIdent
    abstract val behandletTidspunkt: LocalDateTime
    abstract val aarsaker: List<String>
    abstract val forklaring: String?

    @Serializable
    data class Ubesluttet(
        override val behandletAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        override val aarsaker: List<String>,
        override val forklaring: String?,
    ) : Totrinnskontroll()

    @Serializable
    data class Besluttet(
        override val behandletAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val behandletTidspunkt: LocalDateTime,
        val besluttetAv: NavIdent,
        @Serializable(with = LocalDateTimeSerializer::class)
        val besluttetTidspunkt: LocalDateTime,
        val besluttelse: Besluttelse,
        override val aarsaker: List<String>,
        override val forklaring: String?,
    ) : Totrinnskontroll()
}
