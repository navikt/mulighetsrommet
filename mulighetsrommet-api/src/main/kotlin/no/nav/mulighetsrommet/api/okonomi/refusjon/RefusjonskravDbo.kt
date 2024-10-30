package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregning
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import java.time.LocalDateTime
import java.util.*

data class RefusjonskravDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val fristForGodkjenning: LocalDateTime,
    val beregning: RefusjonKravBeregning,
    val kontonummer: Kontonummer?,
    val kid: Kid?,
)
