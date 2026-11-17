package no.nav.tiltak.historikk.replikering

import arrow.core.Either
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.tiltak.historikk.TiltakshistorikkArenaGjennomforing
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.util.UUID

class ArenaGjennomforingReplikering(
    private val db: TiltakshistorikkDatabase,
    private val virksomheter: VirksomhetReplikering,
) {
    suspend fun upsert(input: TiltakshistorikkArenaGjennomforing): Either<BrregError, Unit> {
        val gjennomforing = input.copy(
            arenaRegDato = input.arenaRegDato.truncatedToMicros(),
            arenaModDato = input.arenaModDato.truncatedToMicros(),
        )
        return virksomheter.getOrSyncVirksomhetIfNotExists(gjennomforing.arrangorOrganisasjonsnummer)
            .map {
                db.session {
                    queries.arenaGjennomforing.upsert(gjennomforing)
                }
            }
    }

    fun delete(id: UUID): Unit = db.session {
        queries.arenaGjennomforing.delete(id)
    }
}
