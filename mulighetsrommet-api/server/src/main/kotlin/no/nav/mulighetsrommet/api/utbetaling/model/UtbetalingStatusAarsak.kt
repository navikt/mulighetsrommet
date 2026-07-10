package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable

@Serializable
enum class UtbetalingStatusAarsak(val beskrivelse: String) {
    TILSAGN_GJORT_OPP("Tilsagnsmidler er brukt opp"),
    ANNET("Annet"),
}
