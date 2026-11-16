package no.nav.tiltak.historikk.replikering

import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Tiltakstype

class TiltakstypeReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    fun upsert(tiltakstype: Tiltakstype): Unit = db.session {
        queries.tiltakstype.upsert(tiltakstype)
    }
}
