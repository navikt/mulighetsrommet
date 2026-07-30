package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

data class AttesterUtbetalingLinje(
    val id: UUID,
    val agent: Agent,
)

data class ReturnerUtbetalingLinje(
    val id: UUID,
    val aarsaker: List<UtbetalingLinjeReturnertAarsak>,
    val forklaring: String?,
    val agent: Agent,
)

data class AvbrytUtbetaling(
    val id: UUID,
    val agent: Agent,
    val operation: String,
    val aarsaker: List<UtbetalingStatusAarsak>,
    val forklaring: String?,
)

data class GodkjennAvbrytUtbetaling(
    val id: UUID,
    val agent: Agent,
)

data class AvslaAvbrytUtbetaling(
    val id: UUID,
    val besluttetAv: NavIdent,
    val aarsaker: List<UtbetalingStatusAarsak>,
    val forklaring: String?,
)
