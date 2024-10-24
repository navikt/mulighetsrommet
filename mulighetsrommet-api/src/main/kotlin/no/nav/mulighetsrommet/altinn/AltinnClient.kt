package no.nav.mulighetsrommet.altinn

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.altinn.models.AltinnRessurs
import no.nav.mulighetsrommet.altinn.models.BedriftRettigheter
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class AltinnClient(
    private val baseUrl: String,
    private val tokenProvider: (token: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    data class Config(
        val url: String,
        val scope: String,
    )

    suspend fun hentRettigheter(token: String): List<BedriftRettigheter> {
        log.info("Henter rettigheter fra Altinn for bruker via Team Fager")
        val tilganger = hentTilganger(token)
        return sjekkTilganger(tilganger)
    }

    fun sjekkTilganger(tilganger: Tilgangshierarki): List<BedriftRettigheter> {
        val result = mutableListOf<BedriftRettigheter>()

        fun checkTilganger(org: TilgangForOrganisasjon) {
            if (AltinnRessurs.TILTAK_ARRANGOR_REFUSJON.ressursId in org.altinn3Tilganger) {
                result.add(BedriftRettigheter(Organisasjonsnummer(org.orgnr), listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON)))
            }
            org.underenheter.forEach { checkTilganger(it) }
        }

        tilganger.hierarki.forEach { checkTilganger(it) }
        return result
    }

    private suspend fun hentTilganger(token: String): Tilgangshierarki {
        val response = client.post("$baseUrl/altinn-tilganger") {
            bearerAuth(tokenProvider.invoke(token))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        if (response.status != HttpStatusCode.OK) {
            log.error("Klarte ikke hente organisasjoner fra arbeidsgiver-altinn-tilganger. response: ${response.status}, body=${response.bodyAsText()}")
            throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.status}")
        }

        return response.body()
    }

    @Serializable
    data class Tilgangshierarki(
        val hierarki: List<TilgangForOrganisasjon>,
    )

    @Serializable
    data class TilgangForOrganisasjon(
        val navn: String,
        val orgnr: String,
        val altinn3Tilganger: List<String>,
        val underenheter: List<TilgangForOrganisasjon>,
    )
}
