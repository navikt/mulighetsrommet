package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
enum class Besluttelse {
    GODKJENT,
    AVVIST,
}
