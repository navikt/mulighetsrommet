package no.nav.tiltak.historikk.service

import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import org.slf4j.LoggerFactory

class VirksomhetService(
    private val db: TiltakshistorikkDatabase,
    private val brreg: BrregClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    suspend fun syncVirksomhet(organisasjonsnummer: Organisasjonsnummer) {
        brreg.getBrregEnhet(organisasjonsnummer)
            .onRight { enhet ->
                db.session { queries.virksomhet.upsert(mapBrregEnhetToVirsomhetDbo(enhet)) }
            }
            .onLeft { error ->
                when (error) {
                    is BrregError.FjernetAvJuridiskeArsaker -> {
                        logger.warn("Virksomhet med orgnr=$organisasjonsnummer er fjernet fra Brreg")
                        return
                    }

                    else -> {
                        throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
                    }
                }
            }
    }
}

private fun mapBrregEnhetToVirsomhetDbo(enhet: BrregEnhet): VirksomhetDbo = when (enhet) {
    is BrregHovedenhetDto -> VirksomhetDbo(
        organisasjonsnummer = enhet.organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = enhet.navn,
        organisasjonsform = enhet.organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregHovedenhetDto -> VirksomhetDbo(
        organisasjonsnummer = enhet.organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = enhet.navn,
        organisasjonsform = enhet.organisasjonsform,
        slettetDato = enhet.slettetDato,
    )

    is BrregUnderenhetDto -> VirksomhetDbo(
        organisasjonsnummer = enhet.organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = enhet.overordnetEnhet,
        navn = enhet.navn,
        organisasjonsform = enhet.organisasjonsform,
        slettetDato = null,
    )

    is SlettetBrregUnderenhetDto -> VirksomhetDbo(
        organisasjonsnummer = enhet.organisasjonsnummer,
        overordnetEnhetOrganisasjonsnummer = null,
        navn = enhet.navn,
        organisasjonsform = enhet.organisasjonsform,
        slettetDato = enhet.slettetDato,
    )
}
