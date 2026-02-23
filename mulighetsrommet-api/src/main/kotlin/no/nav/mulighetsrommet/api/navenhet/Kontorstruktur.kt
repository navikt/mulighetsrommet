package no.nav.mulighetsrommet.api.navenhet

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class Kontorstruktur(
    val region: Region,
    val kontorer: List<Kontor>,
) {
    @Serializable
    data class Region(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
    )

    @Serializable
    data class Kontor(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
        val type: Kontortype,
    )

    enum class Kontortype {
        LOKAL,
        SPESIALENHET,
    }

    companion object {
        fun fromNavEnheter(navEnheter: List<NavEnhetDto>): List<Kontorstruktur> {
            val enheterByEnhetsnummer = navEnheter.associateBy { it.enhetsnummer }
            val enheterByOverordnetEnhet = navEnheter.groupBy { it.overordnetEnhet }

            val regioner = enheterByOverordnetEnhet[null].orEmpty()
                .filter { it.type == NavEnhetType.FYLKE && it.enhetsnummer !in enheterByOverordnetEnhet.keys }
                .map { Kontorstruktur(region = Region(it.navn, it.enhetsnummer), kontorer = emptyList()) }

            val kontorstrukturer = enheterByOverordnetEnhet.entries.mapNotNull { (overordnetEnhet, enheter) ->
                overordnetEnhet?.let { enheterByEnhetsnummer[it] }?.let { region ->
                    Kontorstruktur(
                        region = Region(region.navn, region.enhetsnummer),
                        kontorer = enheter.toSet().map { enhet ->
                            val type = if (enhet.type == NavEnhetType.LOKAL) {
                                Kontortype.LOKAL
                            } else {
                                Kontortype.SPESIALENHET
                            }
                            Kontor(enhet.navn, enhet.enhetsnummer, type)
                        },
                    )
                }
            }

            return kontorstrukturer + regioner
        }
    }
}
