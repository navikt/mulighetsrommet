package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorService(
    private val db: ApiDatabase,
    private val brregClient: BrregClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSyncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        return db.session { queries.arrangor.get(orgnr)?.right() } ?: syncArrangorFromBrreg(orgnr)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return brregClient.getBrregVirksomhet(orgnr)
            .flatMap { virksomhet ->
                virksomhet.overordnetEnhet
                    ?.let { getOrSyncArrangorFromBrreg(it).map { virksomhet } }
                    ?: virksomhet.right()
            }
            .map { virksomhet ->
                db.transaction {
                    queries.arrangor.upsert(virksomhet)
                    queries.arrangor.get(virksomhet.organisasjonsnummer)!!
                }
            }
    }

    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson): Unit = db.session {
        queries.arrangor.upsertKontaktperson(kontaktperson)
    }

    fun hentKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> = db.session {
        queries.arrangor.getKontaktpersoner(arrangorId)
    }

    fun hentKoblingerForKontaktperson(kontaktpersonId: UUID): KoblingerForKontaktperson = db.session {
        val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)
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
