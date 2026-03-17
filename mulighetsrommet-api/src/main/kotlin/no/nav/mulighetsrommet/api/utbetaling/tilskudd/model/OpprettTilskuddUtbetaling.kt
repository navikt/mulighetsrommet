package no.nav.mulighetsrommet.api.utbetaling.tilskudd.model

import java.util.UUID

data class OpprettTilskuddUtbetaling(
    val gjennomforingId: UUID,
    val vedtakId: UUID,
    val deltakerId: UUID,
    val tilskuddId: UUID,
    val belop: Int,
)
