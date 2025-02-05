package no.nav.mulighetsrommet.api.refusjon.db

import no.nav.mulighetsrommet.api.refusjon.model.Beregningsmodell
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregning
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDateTime
import java.util.*

data class RefusjonskravDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val fristForGodkjenning: LocalDateTime,
    val beregning: RefusjonKravBeregning,
    val kontonummer: Kontonummer?,
    val kid: Kid?,
    val periode: Periode,
)
