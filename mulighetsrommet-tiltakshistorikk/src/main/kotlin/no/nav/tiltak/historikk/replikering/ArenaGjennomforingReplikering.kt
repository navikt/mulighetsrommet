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
    suspend fun upsert(gjennomforing: TiltakshistorikkArenaGjennomforing): Either<BrregError, Unit> {
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
