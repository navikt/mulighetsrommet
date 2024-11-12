package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilsagnBesluttelseStatus {
    GODKJENT,
    AVVIST,
}
