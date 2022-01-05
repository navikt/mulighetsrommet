package no.nav.mulighetsrommet.api.utils

import kotlinx.serialization.Serializable

// Foreløpig syntetisk event fra Arena. Blir byttet ut med noe mer vettugt når vi vet mer hva Arena sender.
@Serializable
data class ArenaEvent(
    val tiltaksNummer: Int
)
