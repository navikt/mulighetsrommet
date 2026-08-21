package no.nav.tiltak.historikk.service

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

class VirksomhetService(
    private val db: TiltakshistorikkDatabase,
    private val brreg: BrregClient,
) {
    fun getVirksomhet(organisasjonsnummer: Organisasjonsnummer): Virksomhet? = db.session {
        queries.virksomhet.get(organisasjonsnummer)
    }

    fun deleteVirksomhet(organisasjonsnummer: Organisasjonsnummer): Unit = db.session {
        queries.virksomhet.delete(organisasjonsnummer)
    }

    suspend fun getOrSyncVirksomhetIfNotExists(organisasjonsnummer: Organisasjonsnummer): Either<BrregError, Virksomhet> {
        return getVirksomhet(organisasjonsnummer)?.right() ?: return getAndSyncVirksomhet(organisasjonsnummer)
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
