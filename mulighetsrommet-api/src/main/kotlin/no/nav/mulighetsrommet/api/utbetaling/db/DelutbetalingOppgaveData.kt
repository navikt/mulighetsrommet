package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.oppgaver.OppgaveTiltakstype
import java.util.*

data class DelutbetalingOppgaveData(
    val delutbetaling: Delutbetaling,
    val gjennomforingId: UUID,
    val gjennomforingsnavn: String,
    val tiltakstype: OppgaveTiltakstype,
)
