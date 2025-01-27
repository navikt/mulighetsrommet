package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorService(
    private val db: ApiDatabase,
    private val brregClient: BrregClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getArrangor(orgnr: Organisasjonsnummer): ArrangorDto? = db.session {
        queries.arrangor.get(orgnr)
    }

    suspend fun getArrangorOrSyncFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        return getArrangor(orgnr)?.right() ?: syncArrangorFromBrreg(orgnr)
    }

    suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return brregClient.getBrregVirksomhet(orgnr)
            .flatMap { virksomhet ->
                when (virksomhet) {
                    is BrregUnderenhetDto -> getArrangorOrSyncFromBrreg(virksomhet.overordnetEnhet).map { virksomhet }
                    else -> virksomhet.right()
                }
            }
            .map { virksomhet ->
                db.transaction {
                    val id = getArrangor(virksomhet.organisasjonsnummer)?.id ?: UUID.randomUUID()
                    val arrangor = virksomhet.toArrangorDto(id)
                    queries.arrangor.upsert(arrangor)
                    queries.arrangor.getById(id)
                }
            }
    }

    fun deleteArrangor(orgnr: Organisasjonsnummer): Unit = db.session {
        queries.arrangor.delete(orgnr)
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

private fun BrregVirksomhet.toArrangorDto(id: UUID): ArrangorDto {
    return when (this) {
        is BrregEnhetDto, is BrregEnhetMedUnderenheterDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            navn = navn,
            overordnetEnhet = null,
            underenheter = null,
            slettetDato = null,
        )

        is SlettetBrregEnhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            underenheter = null,
        )

        is BrregUnderenhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            navn = navn,
            overordnetEnhet = overordnetEnhet,
            underenheter = null,
            slettetDato = null,
        )

        is SlettetBrregUnderenhet -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            underenheter = null,
        )
    }
}
