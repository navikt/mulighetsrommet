package no.nav.mulighetsrommet.api.kostnadssted

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.model.NavEnhetNummer

class KostnadsstedService(
    private val db: ApiDatabase,
) {
    fun hentKostnadssted(regioner: List<NavEnhetNummer>): List<Kostnadssted> = db.session {
        queries.kostnadssted.getAll(regioner)
    }

    fun hentKostnadssteder(): List<RegionKostnadssteder> = db.session {
        val kostnadssteder = queries.kostnadssted.getAll()
        return RegionKostnadssteder.fromKostnadssteder(kostnadssteder)
    }
}
