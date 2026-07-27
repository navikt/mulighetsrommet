package no.nav.mulighetsrommet.api.janzz

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.api.janzz.client.PamOntologiClient

class PamOntologiService(val pam: PamOntologiClient) {
    suspend fun sokSertifiseringer(sokeStreng: String): List<Sertifisering> {
        val sertifiseringer = sokSertifiseringer(pam, sokeStreng)
            .asSequence()
            .flatMap { typeaheads ->
                typeaheads.map {
                    Sertifisering(
                        konseptId = it.konseptId,
                        label = it.label,
                    )
                }
            }
            // Det finnes noen konsepter med lik label som vi vil filtrere vekk. Det er ikke så
            // viktig hvilken som blir igjen, men sorterer først sånn at det alltid er den samme
            // som blir igjen.
            .sortedBy { it.konseptId }
            .sortedBy { it.label }
            .distinctBy { it.konseptId }
            .distinctBy { it.label }
            .toList()
        return sertifiseringer
    }

    private suspend fun sokSertifiseringer(pam: PamOntologiClient, sok: String) = coroutineScope {
        val autoriseringer = async { pam.sokAutorisasjon(sok) }
        val andreGodkjenninger = async { pam.sokAndreGodkjenninger(sok) }
        awaitAll(autoriseringer, andreGodkjenninger)
    }
}
