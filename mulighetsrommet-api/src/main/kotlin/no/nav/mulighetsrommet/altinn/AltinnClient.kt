package no.nav.mulighetsrommet.altinn

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.M2MTokenProvider
import org.slf4j.LoggerFactory

class AltinnClient(
    private val baseUrl: String,
    private val altinnApiKey: String,
    private val tokenProvider: M2MTokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    data class Config(
        val url: String,
        val apiKey: String,
        val scope: String,
    )

    suspend fun hentRettigheter(norskIdent: NorskIdent): List<BedriftRettigheter> {
        log.info("Henter organisasjoner fra Altinn")
        // TODO: Fiks paginering
        val authorizedParties = hentAuthorizedParties(norskIdent)
        return findAltinnRoller(authorizedParties)
    }

    private fun findAltinnRoller(
        parties: List<AuthorizedParty>,
    ): List<BedriftRettigheter> = parties
        .flatMap { party ->
            findAltinnRoller(party.subunits) +
                BedriftRettigheter(
                    organisasjonsnummer = Organisasjonsnummer(party.organizationNumber),
                    rettigheter = AltinnRessurs
                        .entries
                        .filter { it.ressursId in party.authorizedResources },
                )
        }
        .filter { it.rettigheter.isNotEmpty() }

    private suspend fun hentAuthorizedParties(norskIdent: NorskIdent): List<AuthorizedParty> {
        val response = client.post("$baseUrl/accessmanagement/api/v1/resourceowner/authorizedparties") {
            parameter("includeAltinn2", "true")
            header("Ocp-Apim-Subscription-Key", altinnApiKey)
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                AltinnRequest(
                    type = "urn:altinn:person:identifier-no",
                    value = norskIdent.value,
                ),
            )
        }

        if (response.status != HttpStatusCode.OK) {
            log.error("Klarte ikke hente organisasjoner for Altinn. response: ${response.status}, body=${response.bodyAsText()}")
            throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.status}")
        }

        if (!response.headers["X-Warning-LimitReached"].isNullOrEmpty()) {
            log.error("For mange tilganger. Klarte ikke hente tilganger for bruker. response: ${response.status}")
        }

        return response.body()
    }

    @Serializable
    data class AuthorizedParty(
        val organizationNumber: String,
        @SerialName("name")
        val organizationName: String,
        val type: String,
        val authorizedResources: List<String>,
        val subunits: List<AuthorizedParty>,
    )

    @Serializable
    data class AltinnRequest(
        val type: String,
        val value: String,
    )
}
