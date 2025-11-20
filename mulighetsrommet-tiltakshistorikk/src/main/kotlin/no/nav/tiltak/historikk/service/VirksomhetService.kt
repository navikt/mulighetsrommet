package no.nav.tiltak.historikk.service

import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.db.QueryContext
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo

class VirksomhetService(
    private val db: TiltakshistorikkDatabase,
    private val brreg: BrregClient,
) {
    fun getVirksomhet(organisasjonsnummer: Organisasjonsnummer): VirksomhetDbo? = db.session {
        queries.virksomhet.get(organisasjonsnummer)
    }

    fun deleteVirksomhet(organisasjonsnummer: Organisasjonsnummer): Unit = db.session {
        queries.virksomhet.delete(organisasjonsnummer)
    }

    suspend fun syncVirksomhetIfNotExists(organisasjonsnummer: Organisasjonsnummer) {
        if (getVirksomhet(organisasjonsnummer) == null) {
            syncVirksomhet(organisasjonsnummer)
        }
    }

    suspend fun syncVirksomhet(organisasjonsnummer: Organisasjonsnummer): Unit = db.session {
        if (erUtenlandskVirksomhet(organisasjonsnummer)) {
            return queries.virksomhet.upsert(VirksomhetDbo(organisasjonsnummer, null, null, null, null))
        }

        val error = syncFromBrreg(organisasjonsnummer)
        if (error != null) {
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
    }

    /**
     * Arena oppretter fiktive organisasjonsnummer for utenlandske virksomheter, og disse kan vi identifisere ved at de starter med '1'.
     * Foreløpig lagrer vi disse på samme måte, men unngår å gjøre oppslag mot Brreg. Dette bør forbedres på sikt.
     */
    private fun erUtenlandskVirksomhet(organisasjonsnummer: Organisasjonsnummer): Boolean {
        return organisasjonsnummer.value.first() == '1'
    }

    private suspend fun QueryContext.syncFromBrreg(organisasjonsnummer: Organisasjonsnummer): BrregError? {
        return brreg.getBrregEnhet(organisasjonsnummer)
            .fold({ error ->
                when (error) {
                    is BrregError.FjernetAvJuridiskeArsaker -> {
                        queries.virksomhet.upsert(error.enhet.toVirksomhetDbo())
                        null
                    }

                    else -> error
                }
            }, { enhet ->
                queries.virksomhet.upsert(enhet.toVirksomhetDbo())
                null
            })
    }
}

private fun FjernetBrregEnhetDto.toVirksomhetDbo(): VirksomhetDbo {
    return VirksomhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = null,
        organisasjonsform = null,
        slettetDato = slettetDato,
    )
}

private fun BrregEnhet.toVirksomhetDbo(): VirksomhetDbo = when (this) {
    is BrregHovedenhetDto -> VirksomhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregHovedenhetDto -> VirksomhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )

    is BrregUnderenhetDto -> VirksomhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = overordnetEnhet,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregUnderenhetDto -> VirksomhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )
}
