package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingDbo(
    val id: UUID,
    val innsender: Agent?,
    val gjennomforingId: UUID,
    val status: UtbetalingStatusType,
    val valuta: Valuta,
    val beregning: UtbetalingBeregning,
    val betalingsinformasjon: Betalingsinformasjon?,
    val periode: Periode,
    val beskrivelse: String?,
    val tilskuddstype: Tilskuddstype,
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    val utbetalesTidligstTidspunkt: Instant?,
    val blokkeringer: Set<Utbetaling.Blokkering>,
)
