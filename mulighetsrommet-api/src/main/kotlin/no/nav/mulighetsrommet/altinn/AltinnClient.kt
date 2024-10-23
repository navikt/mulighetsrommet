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
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class AltinnClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
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

    suspend fun hentRettigheter(): List<BedriftRettigheter> {
        log.info("Henter organisasjoner fra Altinn")
        val tilganger = hentTilganger()
        return findAltinnRoller(tilganger)
    }

    private fun findAltinnRoller(
        bedriftsrettigheter: List<BedriftRettigheter>,
    ): List<BedriftRettigheter> =
        bedriftsrettigheter
            .flatMap { rettighet ->
                findAltinnRoller(rettighet.) +
                    BedriftRettigheter(
                        organisasjonsnummer = Organisasjonsnummer(rettighet.organizationNumber),
                        rettigheter = AltinnRessurs
                            .entries
                            .filter { it.ressursId in rettighet.authorizedResources },
                    )
            }
            .filter { it.rettigheter.isNotEmpty() }

    private suspend fun hentTilganger(): Tilgangshierarki {
        val response = client.post("$baseUrl/altinn-tilganger") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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
