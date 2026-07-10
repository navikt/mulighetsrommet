package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class KostnadsstedDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
) {
    companion object {
        fun fromNavEnhet(it: NavEnhet) = KostnadsstedDto(it.navn, it.enhetsnummer)
    }
}
