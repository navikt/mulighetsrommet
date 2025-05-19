package no.nav.mulighetsrommet.altinn.db

import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import java.time.Instant

data class BedriftRettighetWithExpiry(
    val rettighet: AltinnRessurs,
    val expiry: Instant,
)
