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
                    .mapNotNull {
                        NavRegionUnderenhetDto(
                            navn = it.navn,
                            enhetsnummer = it.enhetsnummer,
                            overordnetEnhet = it.overordnetEnhet ?: return@mapNotNull null,
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
        return enhetsnummer.value in NAV_EGNE_ANSATTE_TIL_FYLKE_MAP.keys + NAV_ARBEID_OG_HELSE_TIL_FYLKE_MAP.keys
    }
}
