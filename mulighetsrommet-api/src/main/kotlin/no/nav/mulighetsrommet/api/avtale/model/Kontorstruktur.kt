package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo

@Serializable
data class Kontorstruktur(
    val region: NavEnhetDbo,
    val kontorer: List<NavEnhetDbo>,
) {
    companion object {
        fun fromNavEnheter(navEnheter: List<NavEnhetDbo>): List<Kontorstruktur> {
            val enheterByEnhetsnummer = navEnheter.associateBy { it.enhetsnummer }
            val enheterByOverordnetEnhet = navEnheter.groupBy { it.overordnetEnhet }

            val regioner = enheterByOverordnetEnhet[null].orEmpty()
                .filter { it.enhetsnummer !in enheterByOverordnetEnhet.keys }
                .map { Kontorstruktur(region = it, kontorer = emptyList()) }

            val kontorstrukturer = enheterByOverordnetEnhet.entries.mapNotNull { (overordnetEnhet, enheter) ->
                overordnetEnhet?.let {
                    Kontorstruktur(region = enheterByEnhetsnummer.getValue(it), kontorer = enheter)
                }
            }

            return kontorstrukturer + regioner
        }
    }
}
