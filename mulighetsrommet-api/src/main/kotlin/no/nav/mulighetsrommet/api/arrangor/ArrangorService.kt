package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorService(
    private val brregClient: BrregClient,
    private val arrangorRepository: ArrangorRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSyncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        return arrangorRepository.get(orgnr)?.right() ?: syncArrangorFromBrreg(orgnr)
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
                arrangorRepository.upsert(virksomhet)
                arrangorRepository.get(virksomhet.organisasjonsnummer)!!
            }
    }

    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson) =
        arrangorRepository.upsertKontaktperson(kontaktperson)

    fun hentKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> =
        arrangorRepository.getKontaktpersoner(arrangorId)

    fun hentKoblingerForKontaktperson(kontaktpersonId: UUID): StatusResponse<KoblingerForKontaktperson> {
        val (gjennomforinger, avtaler) = arrangorRepository.koblingerTilKontaktperson(kontaktpersonId)
        return Either.Right(
            KoblingerForKontaktperson(
                gjennomforinger = gjennomforinger,
                avtaler = avtaler,
            ),
        )
    }

    fun deleteKontaktperson(kontaktpersonId: UUID): StatusResponse<Unit> {
        val (gjennomforinger, avtaler) = arrangorRepository.koblingerTilKontaktperson(kontaktpersonId)
        if (gjennomforinger.isNotEmpty()) {
            log.warn("Prøvde slette kontaktperson med koblinger til disse gjennomføringer: ${gjennomforinger.joinToString { "${it.navn} (${it.id})" }}")
            return Either.Left(BadRequest("Kontaktpersonen er i bruk."))
        }
        if (avtaler.isNotEmpty()) {
            log.warn("Prøvde slette kontaktperson med koblinger til disse avtaler: ${avtaler.joinToString { "${it.navn} (${it.id})" }}")
            return Either.Left(BadRequest("Kontaktpersonen er i bruk."))
        }

        return Either.Right(arrangorRepository.deleteKontaktperson(kontaktpersonId))
    }
}

@Serializable
data class KoblingerForKontaktperson(
    val gjennomforinger: List<DokumentKoblingForKontaktperson>,
    val avtaler: List<DokumentKoblingForKontaktperson>,
)
