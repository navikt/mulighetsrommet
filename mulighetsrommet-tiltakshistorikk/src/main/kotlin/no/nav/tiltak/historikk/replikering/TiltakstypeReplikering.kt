package no.nav.tiltak.historikk.replikering

import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Tiltakstype

class TiltakstypeReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    fun upsert(input: Tiltakstype): Unit = db.session {
        val tiltakstype = input.copy(
            opprettetTidspunkt = input.opprettetTidspunkt?.truncatedToMicros(),
            oppdatertTidspunkt = input.oppdatertTidspunkt?.truncatedToMicros(),
        )
        queries.tiltakstype.upsert(tiltakstype)
    }
}
