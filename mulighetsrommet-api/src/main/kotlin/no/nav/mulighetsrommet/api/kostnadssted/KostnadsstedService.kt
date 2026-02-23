package no.nav.mulighetsrommet.api.kostnadssted

import no.nav.mulighetsrommet.api.ApiDatabase

class KostnadsstedService(
    private val db: ApiDatabase,
) {
    fun hentKostnadssteder(): List<RegionKostnadssteder> = db.session {
        val kostnadssteder = queries.kostnadssted.getAll()
        return RegionKostnadssteder.fromKostnadssteder(kostnadssteder)
    }
}
