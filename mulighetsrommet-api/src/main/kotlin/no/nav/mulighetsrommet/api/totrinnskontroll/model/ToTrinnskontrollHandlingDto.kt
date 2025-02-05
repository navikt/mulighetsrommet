package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.ToTrinnskontrollHandling
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class ToTrinnskontrollHandlingDto<T>(
    val opprettetAv: NavIdent,
    val opprettetAvNavn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val handling: ToTrinnskontrollHandling,
    val aarsaker: List<T>,
    val forklaring: String?,
)
