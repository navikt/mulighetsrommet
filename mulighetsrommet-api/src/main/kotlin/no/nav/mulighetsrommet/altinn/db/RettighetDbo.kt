package no.nav.mulighetsrommet.altinn.db

import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import java.time.LocalDateTime

data class RettighetDbo(
    val rettighet: AltinnRessurs,
    val expiry: LocalDateTime,
)
