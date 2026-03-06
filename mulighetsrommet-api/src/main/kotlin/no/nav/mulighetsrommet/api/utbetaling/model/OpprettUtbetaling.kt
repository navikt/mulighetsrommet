package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

data class OpprettUtbetaling(
    val id: UUID,
    val gjennomforingId: UUID,
    val periode: Periode,
    val journalpostId: JournalpostId?,
    val beregning: UtbetalingBeregning,
    val kommentar: String?,
    val korreksjonGjelderUtbetalingId: UUID?,
    val korreksjonBegrunnelse: String?,
    val kid: Kid?,
    val tilskuddstype: Tilskuddstype,
    val vedlegg: List<Vedlegg>,
)
