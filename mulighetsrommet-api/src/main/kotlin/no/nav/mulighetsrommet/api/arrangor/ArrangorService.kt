package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorService(
    private val db: ApiDatabase,
    private val brregClient: BrregClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun brregSok(sok: String): StatusResponse<List<BrregHovedenhet>> {
        if (sok.isBlank()) {
            return BadRequest("'sok' kan ikke være en tom streng").left()
        }

        return brregClient.sokHovedenhet(sok)
            .map { hovedenheter ->
                val utenlandskeVirksomheter = db.session {
                    queries.arrangor.getAll(sok = sok, utenlandsk = true).items.map {
                        toBrregHovedenhet(it)
                    }
                }
                // Kombinerer resultat med utenlandske virksomheter siden de ikke finnes i brreg
                hovedenheter + utenlandskeVirksomheter
            }
            .mapLeft { toStatusResponseError(it) }
    }

    suspend fun brregUnderenheter(orgnr: Organisasjonsnummer): StatusResponse<List<BrregUnderenhet>> {
        if (isUtenlandskOrgnr(orgnr)) {
            val arrangor = db.session { queries.arrangor.get(orgnr) }
            return if (arrangor != null) {
                listOf(
                    BrregUnderenhetDto(
                        organisasjonsnummer = arrangor.organisasjonsnummer,
                        organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
                        navn = arrangor.navn,
                        overordnetEnhet = arrangor.organisasjonsnummer,
                    ),
                ).right()
            } else {
                NotFound("Fant ikke enhet med orgnr: $orgnr").left()
            }
        }

        return brregClient.getUnderenheterForHovedenhet(orgnr)
            .map { underenheter ->
                val slettedeVirksomheter = db.session {
                    queries.arrangor.getAll(overordnetEnhetOrgnr = orgnr, slettet = true).items.map {
                        toBrregUnderenhet(it)
                    }
                }
                // Kombinerer resultat med virksomheter som er slettet fra brreg for å støtte avtaler/gjennomføringer som henger etter
                underenheter + slettedeVirksomheter
            }
            .mapLeft { toStatusResponseError(it) }
    }

    fun getArrangor(orgnr: Organisasjonsnummer): ArrangorDto? = db.session {
        queries.arrangor.get(orgnr)
    }

    suspend fun getArrangorOrSyncFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        return getArrangor(orgnr)?.right() ?: syncArrangorFromBrreg(orgnr)
    }

    suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<BrregError, ArrangorDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return brregClient.getBrregEnhet(orgnr)
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

private fun BrregEnhet.toArrangorDto(id: UUID): ArrangorDto {
    return when (this) {
        is BrregHovedenhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            overordnetEnhet = null,
            underenheter = null,
            slettetDato = null,
        )

        is SlettetBrregHovedenhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            underenheter = null,
        )

        is BrregUnderenhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            overordnetEnhet = overordnetEnhet,
            underenheter = null,
            slettetDato = null,
        )

        is SlettetBrregUnderenhetDto -> ArrangorDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            underenheter = null,
        )
    }
}

fun isUtenlandskOrgnr(orgnr: Organisasjonsnummer): Boolean {
    return orgnr.value.matches("^[1-7][0-9]{8}\$".toRegex())
}

private fun toBrregHovedenhet(arrangor: ArrangorDto): BrregHovedenhet = when {
    arrangor.slettetDato != null -> SlettetBrregHovedenhetDto(
        organisasjonsnummer = arrangor.organisasjonsnummer,
        organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
        navn = arrangor.navn,
        slettetDato = arrangor.slettetDato,
    )

    else -> BrregHovedenhetDto(
        organisasjonsnummer = arrangor.organisasjonsnummer,
        organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
        navn = arrangor.navn,
        postadresse = null,
        forretningsadresse = null,
    )
}

private fun toBrregUnderenhet(arrangor: ArrangorDto): BrregUnderenhet {
    requireNotNull(arrangor.overordnetEnhet)
    return when {
        arrangor.slettetDato != null -> SlettetBrregUnderenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            slettetDato = arrangor.slettetDato,
        )

        else -> BrregUnderenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            overordnetEnhet = arrangor.overordnetEnhet,
        )
    }
}
