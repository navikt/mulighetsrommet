package no.nav.tiltak.historikk.replikering

import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.util.UUID

class ArenaDeltakerReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    fun upsert(deltaker: TiltakshistorikkArenaDeltaker): Unit = db.session {
        queries.arenaDeltaker.upsertArenaDeltaker(deltaker)
    }

    fun delete(id: UUID): Unit = db.session {
        queries.arenaDeltaker.deleteArenaDeltaker(id)
    }
}
