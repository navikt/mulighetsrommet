package no.nav.tiltak.historikk.replikering

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Gjennomforing
import java.util.UUID

class GjennomforingReplikering(
    private val db: TiltakshistorikkDatabase,
    private val virksomheter: VirksomhetReplikering,
) {
    suspend fun upsert(gjennomforing: Gjennomforing) {
        val organisasjonsnummer = Organisasjonsnummer(gjennomforing.arrangorOrganisasjonsnummer)
        virksomheter.getOrSyncVirksomhetIfNotExists(organisasjonsnummer).onLeft { error ->
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
        db.session {
            queries.gjennomforing.upsert(gjennomforing)
        }
    }

    fun delete(id: UUID): Unit = db.session {
        queries.gjennomforing.delete(id)
    }
}
