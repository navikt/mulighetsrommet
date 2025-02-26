package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Totrinnskontroll(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val behandletAv: NavIdent,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val aarsaker: List<String>,
    val forklaring: String?,
    val besluttetAv: NavIdent?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime?,
    val besluttelse: Besluttelse?,
)
