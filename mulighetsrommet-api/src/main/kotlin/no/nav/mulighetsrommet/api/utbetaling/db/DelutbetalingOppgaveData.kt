package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.*

data class DelutbetalingOppgaveData(
    val delutbetaling: DelutbetalingDto,
    val gjennomforingId: UUID,
    val tiltakskode: Tiltakskode,
    val gjennomforingsnavn: String,
)
