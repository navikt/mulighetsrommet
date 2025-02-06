package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class ToTrinnskontroll<T>(
    val opprett: Opprett,
    val beslutt: Beslutt?,
    val aarsaker: List<T>,
    val forklaring: String?,
) {
    @Serializable
    data class Opprett(
        val navIdent: NavIdent,
        val navn: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
    )

    @Serializable
    data class Beslutt(
        val navIdent: NavIdent,
        val navn: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val besluttelse: Besluttelse,
    )
}
