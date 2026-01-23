package no.nav.mulighetsrommet.api.kostnadssted

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.navenhet.NavRegionUnderenhetDto
import no.nav.mulighetsrommet.model.NavEnhetNummer

class KostnadsstedService(
    private val db: ApiDatabase,
) {
    fun hentKostnadssted(regioner: List<NavEnhetNummer>): List<Kostnadssted> = db.session {
        queries.kostnadssted.getAll(regioner)
    }

    fun hentKostnadsstedFilter(): List<NavRegionDto> = db.session {
        return queries.kostnadssted.getAll()
            .groupBy { it.region }
            .map { (region, kostnadssteder) ->
                val enheter = kostnadssteder.map {
                    NavRegionUnderenhetDto(
                        navn = it.navn,
                        enhetsnummer = it.enhetsnummer,
                        erStandardvalg = true,
                    )
                }
                NavRegionDto(
                    enhetsnummer = region.enhetsnummer,
                    navn = region.navn,
                    enheter = enheter,
                )
            }
    }
}
