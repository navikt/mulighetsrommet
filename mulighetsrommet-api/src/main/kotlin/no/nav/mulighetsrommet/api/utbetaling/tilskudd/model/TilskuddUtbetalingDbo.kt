package no.nav.mulighetsrommet.api.utbetaling.tilskudd.model

import java.util.UUID

data class TilskuddUtbetalingDbo(
    val id: UUID,
    val vedtakId: UUID,
    val lopenummer: Int,
    val tiltakstypeId: UUID,
    val tilskuddId: UUID,
    val belop: Int,
    val totrinnskontrollId: UUID,
)
