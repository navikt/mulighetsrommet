package no.nav.mulighetsrommet.api.gjennomforing.db

import java.util.UUID

data class EnkeltplassPrisendringDbo(
    val totrinnskontrollId: UUID,
    val gjennomforingId: UUID,
    val prismodellId: UUID,
)
