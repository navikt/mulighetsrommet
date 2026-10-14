package no.nav.mulighetsrommet.api.persistence.gjennomforing

import java.util.UUID

data class EnkeltplassPrisendringDbo(
    val totrinnskontrollId: UUID,
    val gjennomforingId: UUID,
    val prismodellId: UUID,
)
