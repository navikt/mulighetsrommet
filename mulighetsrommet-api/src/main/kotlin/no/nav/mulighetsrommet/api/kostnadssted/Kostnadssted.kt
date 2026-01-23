package no.nav.mulighetsrommet.api.kostnadssted

import no.nav.mulighetsrommet.model.NavEnhetNummer

data class Kostnadssted(
    val enhetsnummer: NavEnhetNummer,
    val navn: String,
    val region: Region,
) {
    data class Region(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}
