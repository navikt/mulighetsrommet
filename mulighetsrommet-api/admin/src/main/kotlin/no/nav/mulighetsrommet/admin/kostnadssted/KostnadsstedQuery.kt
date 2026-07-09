package no.nav.mulighetsrommet.admin.kostnadssted

import no.nav.mulighetsrommet.admin.AdminDatabase

class KostnadsstedQuery(
    private val db: AdminDatabase,
) {
    fun execute(): List<RegionKostnadssteder> = db.session {
        val kostnadssteder = queries.kostnadssted.getAll()
        RegionKostnadssteder.fromKostnadssteder(kostnadssteder)
    }
}
