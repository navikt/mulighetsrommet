package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorService(
    private val db: ApiDatabase,
    private val brregClient: BrregClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSyncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        return db.session { Queries.arrangor.get(orgnr)?.right() } ?: syncArrangorFromBrreg(orgnr)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return brregClient.getBrregVirksomhet(orgnr)
            .flatMap { virksomhet ->
                if (virksomhet.overordnetEnhet == null) {
                    virksomhet.right()
                } else {
                    getOrSyncArrangorFromBrreg(virksomhet.overordnetEnhet).map { virksomhet }
                }
            }
            .map { virksomhet ->
                db.tx {
                    Queries.arrangor.upsert(virksomhet)
                    Queries.arrangor.get(virksomhet.organisasjonsnummer)!!
                }
            }
    }

    // TODO inline
    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson) = db.session {
        Queries.arrangor.upsertKontaktperson(kontaktperson)
    }

    // TODO inline
    fun hentKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> = db.session {
        Queries.arrangor.getKontaktpersoner(arrangorId)
    }

    // TODO inline
    fun hentKoblingerForKontaktperson(kontaktpersonId: UUID): KoblingerForKontaktperson = db.session {
        val (gjennomforinger, avtaler) = Queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)
        KoblingerForKontaktperson(
            gjennomforinger = gjennomforinger,
            avtaler = avtaler,
        )
    }
}

@Serializable
data class KoblingerForKontaktperson(
    val gjennomforinger: List<DokumentKoblingForKontaktperson>,
    val avtaler: List<DokumentKoblingForKontaktperson>,
)
