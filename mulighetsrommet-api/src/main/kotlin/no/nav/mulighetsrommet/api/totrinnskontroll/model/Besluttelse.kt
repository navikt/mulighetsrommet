package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.Serializable

@Serializable
enum class Besluttelse {
    GODKJENT,
    AVVIST,
}
