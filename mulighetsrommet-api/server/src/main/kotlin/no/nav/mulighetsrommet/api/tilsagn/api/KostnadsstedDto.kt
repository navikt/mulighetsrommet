package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class KostnadsstedDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
) {
    companion object {
        fun fromNavEnhetDbo(it: NavEnhetDbo) = KostnadsstedDto(it.navn, it.enhetsnummer)
    }
}
