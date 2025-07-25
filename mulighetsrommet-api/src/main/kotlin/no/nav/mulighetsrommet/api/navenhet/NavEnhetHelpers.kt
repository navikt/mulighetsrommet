package no.nav.mulighetsrommet.api.navenhet

import no.nav.mulighetsrommet.model.NavEnhetNummer

object NavEnhetHelpers {
    fun erGeografiskEnhet(type: NavEnhetType): Boolean {
        return type == NavEnhetType.FYLKE || type == NavEnhetType.LOKAL
    }

    fun erSpesialenhetSomKanVelgesIModia(enhetsnummer: NavEnhetNummer): Boolean {
        return enhetsnummer.value in NAV_EGNE_ANSATTE_TIL_FYLKE_MAP.keys + NAV_ARBEID_OG_HELSE_TIL_FYLKE_MAP.keys
    }
}
