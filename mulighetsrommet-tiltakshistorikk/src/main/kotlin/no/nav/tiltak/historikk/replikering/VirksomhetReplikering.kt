package no.nav.tiltak.historikk.replikering

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregEnhet
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.FjernetBrregEnhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.db.QueryContext
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Virksomhet
import org.slf4j.LoggerFactory

class VirksomhetReplikering(
    private val db: TiltakshistorikkDatabase,
    private val brreg: BrregClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVirksomhet(organisasjonsnummer: Organisasjonsnummer): Virksomhet? = db.session {
        queries.virksomhet.get(organisasjonsnummer)
    }

    fun deleteVirksomhet(organisasjonsnummer: Organisasjonsnummer): Unit = db.session {
        logger.info("Sletter virksomhet med orgnr=$organisasjonsnummer")
        queries.virksomhet.delete(organisasjonsnummer)
    }

    suspend fun syncVirksomhetIfExists(organisasjonsnummer: Organisasjonsnummer) {
        if (getVirksomhet(organisasjonsnummer) == null) {
            return
        }

        getAndSyncVirksomhet(organisasjonsnummer).onLeft { error ->
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
    }

    suspend fun getOrSyncVirksomhetIfNotExists(organisasjonsnummer: Organisasjonsnummer): Either<BrregError, Virksomhet> {
        return getVirksomhet(organisasjonsnummer)?.right() ?: getAndSyncVirksomhet(organisasjonsnummer)
    }

    suspend fun getAndSyncVirksomhet(organisasjonsnummer: Organisasjonsnummer): Either<BrregError, Virksomhet> = db.session {
        if (erUtenlandskVirksomhet(organisasjonsnummer)) {
            return Virksomhet(organisasjonsnummer, null, null, null, null)
                .also { queries.virksomhet.upsert(it) }
                .right()
        }

        return syncFromBrreg(organisasjonsnummer)
    }

    /**
     * TODO: bedre løsning for utenlandske virksomheter
     * Arena oppretter fiktive organisasjonsnummer for utenlandske virksomheter, og disse kan vi identifisere ved at de starter med '1'.
     * Foreløpig lagrer vi disse på samme måte, men unngår å gjøre oppslag mot Brreg. Dette bør forbedres på sikt.
     */
    private fun erUtenlandskVirksomhet(organisasjonsnummer: Organisasjonsnummer): Boolean {
        return organisasjonsnummer.value.first() == '1'
    }

    private suspend fun QueryContext.syncFromBrreg(organisasjonsnummer: Organisasjonsnummer): Either<BrregError, Virksomhet> {
        return brreg.getBrregEnhet(organisasjonsnummer)
            .flatMap { enhet ->
                val overordnetEnhet = when (enhet) {
                    is BrregHovedenhetDto -> enhet.overordnetEnhet

                    is BrregUnderenhetDto -> enhet.overordnetEnhet

                    is SlettetBrregHovedenhetDto,
                    is SlettetBrregUnderenhetDto,
                    -> null
                }
                overordnetEnhet?.let { getOrSyncVirksomhetIfNotExists(it).map { enhet } } ?: enhet.right()
            }
            .fold({ error ->
                when (error) {
                    is BrregError.FjernetAvJuridiskeArsaker -> {
                        error.enhet.toVirksomhetDbo().also { queries.virksomhet.upsert(it) }.right()
                    }

                    else -> error.left()
                }
            }, { enhet ->
                enhet.toVirksomhetDbo().also { queries.virksomhet.upsert(it) }.right()
            })
    }
}

private fun FjernetBrregEnhetDto.toVirksomhetDbo(): Virksomhet {
    return Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = null,
        organisasjonsform = null,
        slettetDato = slettetDato,
    )
}

private fun BrregEnhet.toVirksomhetDbo(): Virksomhet = when (this) {
    is BrregHovedenhetDto -> Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregHovedenhetDto -> Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )

    is BrregUnderenhetDto -> Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = overordnetEnhet,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregUnderenhetDto -> Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )
}
