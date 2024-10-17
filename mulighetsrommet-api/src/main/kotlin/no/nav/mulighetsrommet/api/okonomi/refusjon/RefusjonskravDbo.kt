package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregning
import java.time.LocalDateTime
import java.util.*

data class RefusjonskravDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val fristForGodkjenning: LocalDateTime,
    val beregning: RefusjonKravBeregning,
)
