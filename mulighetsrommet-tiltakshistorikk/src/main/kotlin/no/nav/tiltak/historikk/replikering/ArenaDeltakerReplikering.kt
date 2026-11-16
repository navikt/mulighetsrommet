package no.nav.tiltak.historikk.replikering

import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.util.UUID

class ArenaDeltakerReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    fun upsert(input: TiltakshistorikkArenaDeltaker): Unit = db.session {
        val deltaker = input.copy(
            arenaRegDato = input.arenaRegDato.truncatedToMicros(),
            arenaModDato = input.arenaModDato.truncatedToMicros(),
        )
        queries.arenaDeltaker.upsertArenaDeltaker(deltaker)
    }

    fun delete(id: UUID): Unit = db.session {
        queries.arenaDeltaker.deleteArenaDeltaker(id)
    }
}
