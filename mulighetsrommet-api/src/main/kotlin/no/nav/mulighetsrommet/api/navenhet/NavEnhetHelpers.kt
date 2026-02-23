package no.nav.mulighetsrommet.api.navenhet

import no.nav.mulighetsrommet.model.NavEnhetNummer

object NavEnhetHelpers {
    fun buildNavRegioner(enheter: List<NavEnhetDto>): List<NavRegionDto> {
        return enheter
            .filter { it.type == NavEnhetType.FYLKE }
            .toSet()
            .map { region ->
                val underliggendeEnheter = enheter
                    .filter { it.overordnetEnhet == region.enhetsnummer }
                    .toSet()
                    .map {
                        NavRegionUnderenhetDto(
                            navn = it.navn,
                            enhetsnummer = it.enhetsnummer,
                            erStandardvalg = it.type == NavEnhetType.LOKAL,
                        )
                    }
                    .sortedByDescending { it.erStandardvalg }

                NavRegionDto(
                    enhetsnummer = region.enhetsnummer,
                    navn = region.navn,
                    enheter = underliggendeEnheter,
                )
            }
    }

    fun erGeografiskEnhet(type: NavEnhetType): Boolean {
        return type == NavEnhetType.FYLKE || type == NavEnhetType.LOKAL
    }

    fun erSpesialenhetSomKanVelgesIModia(enhetsnummer: NavEnhetNummer): Boolean {
        return enhetsnummer.value in SPESIALENHET_SOM_KAN_VELGES_I_MODIA_TIL_FYLKE_MAP.keys
    }
}

/**
 * Noen Nav-enheter (som ikke er av typen LOKAL) skal likevel være mulig å velge som tilgjengelighet til
 * Tiltaksadministrasjon/Modia.
 *
 * "Foreløpig" løsning er at vi vedlikeholder en slik mapping med spesialenheter som skal kunne velges som tilhørlighet
 * på avtaler/gjennomføringer. I tillegg er det også en begrensning til at disse må mappes til et fylke slik at de kan
 * vises riktig i filter i frontend.
 */
val SPESIALENHET_SOM_KAN_VELGES_I_MODIA_TIL_FYLKE_MAP = mapOf(
    // Nasjonal oppfølgingsenhet
    "4154" to "0300",
    // Nav egne ansatte Vestfold og Telemark
    "0883" to "0800",
    // Nav egne ansatte Vestland
    "1283" to "1200",
    // Nav egne ansatte Troms og Finnmark
    "1983" to "1900",
    // Nav egne ansatte Oslo
    "0383" to "0300",
    // Nav egne ansatte Rogaland
    "1183" to "1100",
    // Nav egne ansatte Møre og Romsdal
    "1583" to "1500",
    // Nav egne ansatte Vest-Viken
    "0683" to "0600",
    // Nav egne ansatte Agder
    "1083" to "1000",
    // Nav egne ansatte Nordland
    "1883" to "1800",
    // Nav egne ansatte Øst-Viken
    "0283" to "0200",
    // Nav egne ansatte Innlandet
    "0483" to "0400",
    // Nav egne ansatte Trøndelag
    "1683" to "5700",
    // Nav arbeid og helse Oslo
    "0396" to "0300",
)
