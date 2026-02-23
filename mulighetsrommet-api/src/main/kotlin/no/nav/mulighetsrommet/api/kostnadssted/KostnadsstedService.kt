package no.nav.mulighetsrommet.api.kostnadssted

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.navenhet.NavRegionUnderenhetDto

class KostnadsstedService(
    private val db: ApiDatabase,
) {
    fun hentKostnadssteder(): List<RegionKostnadssteder> = db.session {
        val kostnadssteder = queries.kostnadssted.getAll()
        return RegionKostnadssteder.fromKostnadssteder(kostnadssteder)
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
