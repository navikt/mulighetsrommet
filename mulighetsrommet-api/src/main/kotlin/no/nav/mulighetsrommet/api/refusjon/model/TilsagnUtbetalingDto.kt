package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class TilsagnUtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val refusjonskravId: UUID,
    val opprettetAv: NavIdent,
    val besluttetAv: NavIdent?,
    val belop: Int,
)
