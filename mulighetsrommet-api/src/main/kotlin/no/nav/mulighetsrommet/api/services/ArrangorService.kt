package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.repositories.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.StatusResponse
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val brregClient: BrregClient,
    private val arrangorRepository: ArrangorRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val brregCache: Cache<String, BrregVirksomhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.HOURS)
        .maximumSize(20_000)
        .recordStats()
        .build()

    suspend fun getOrSyncArrangorFromBrreg(orgnr: String): Either<BrregError, ArrangorDto> {
        return arrangorRepository.get(orgnr)?.right() ?: syncArrangorFromBrreg(orgnr)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: String): Either<BrregError, ArrangorDto> {
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
