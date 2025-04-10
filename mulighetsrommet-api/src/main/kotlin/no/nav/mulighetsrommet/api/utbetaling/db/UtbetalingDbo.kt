package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.*

data class UtbetalingDbo(
    val id: UUID,
    val innsender: Agent?,
    val gjennomforingId: UUID,
    val fristForGodkjenning: LocalDateTime,
    val beregning: UtbetalingBeregning,
    val kontonummer: Kontonummer?,
    val kid: Kid?,
    val periode: Periode,
    val beskrivelse: String?,
    val tilskuddstype: Tilskuddstype,
)
