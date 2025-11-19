package no.nav.tiltak.historikk.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotliquery.queryOf
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.db.QueryContext
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import org.intellij.lang.annotations.Language
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

    suspend fun syncVirksomhet(organisasjonsnummer: Organisasjonsnummer) = db.session {
        val error = syncFromBrreg(organisasjonsnummer)
        if (error != null) {
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
    }

    fun syncAlleVirksomheterUtenNavn(scope: CoroutineScope) {
        scope.launch {
            var processed: Int
            do {
                processed = db.transaction {
                    @Language("PostgreSQL")
                    val query = """
                    select organisasjonsnummer
                    from virksomhet
                    where navn is null and (organisasjonsnummer like '8%' or organisasjonsnummer like '9%')
                    order by organisasjonsnummer
                    limit 100
                    for update skip locked
                    """.trimIndent()

                    val orgnrs = session.list(queryOf(query)) { Organisasjonsnummer(it.string("organisasjonsnummer")) }
                    orgnrs.forEach { syncFromBrreg(it) }
                    orgnrs.size
                }
            } while (processed > 0)
        }
    }

    private suspend fun QueryContext.syncFromBrreg(organisasjonsnummer: Organisasjonsnummer): BrregError? {
        return brreg.getBrregEnhet(organisasjonsnummer)
            .fold({ error ->
                when (error) {
                    is BrregError.FjernetAvJuridiskeArsaker -> {
                        logger.warn("Virksomhet med orgnr=$organisasjonsnummer er fjernet fra Brreg")
                        null
                    }

                    else -> error
                }
            }, { enhet ->
                queries.virksomhet.upsert(mapBrregEnhetToVirsomhetDbo(enhet))
                null
            })
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
