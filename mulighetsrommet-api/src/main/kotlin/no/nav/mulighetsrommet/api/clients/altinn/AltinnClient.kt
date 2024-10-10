package no.nav.mulighetsrommet.api.clients.altinn

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.M2MTokenProvider
import org.slf4j.LoggerFactory

const val PAGINERING_SIZE = 500

class AltinnClient(
    private val baseUrl: String,
    private val altinnApiKey: String,
    private val tokenProvider: M2MTokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
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

    suspend fun hentRoller(norskIdent: NorskIdent): List<AltinnRolle> {
        val roller: MutableList<AltinnRolle> = mutableListOf()
        var ferdig = false
        while (!ferdig) {
            log.info("Henter organisasjoner fra Altinn")
            val authorizedParties = hentAuthorizedParties(norskIdent)
            roller.addAll(findAltinnRoller(authorizedParties, listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON)))
            ferdig = roller.size < PAGINERING_SIZE
        }
        return roller
    }

    private fun findAltinnRoller(
        parties: List<AuthorizedParty>,
        ressurser: List<AltinnRessurs>,
        roller: MutableList<AltinnRolle> = mutableListOf(),
    ): List<AltinnRolle> {
        for (party in parties) {
            if (party.organizationNumber != null) {
                roller.addAll(
                    ressurser
                        .filter { party.authorizedResources.contains(it.ressursId) }
                        .map {
                            AltinnRolle(
                                organisasjonsnummer = Organisasjonsnummer(party.organizationNumber),
                                ressurs = it,
                            )
                        },
                )
            }
            findAltinnRoller(party.subunits, ressurser, roller)
        }
        return roller
    }

    private suspend fun hentAuthorizedParties(norskIdent: NorskIdent): List<AuthorizedParty> {
        @Serializable
        data class Request(
            val type: String,
            val value: String,
        )
        val response = client.post("$baseUrl/accessmanagement/api/v1/resourceowner/authorizedparties") {
            parameter("includeAltinn2", "true")
            header("Ocp-Apim-Subscription-Key", altinnApiKey)
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                Request(
                    type = "urn:altinn:person:identifier-no",
                    value = norskIdent.value,
                ),
            )
        }

        if (response.status != HttpStatusCode.OK) {
            log.error("Klarte ikke hente organisasjoner for Altinn. response: ${response.status}")
            throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.status}")
        }

        if (!response.headers["X-Warning-LimitReached"].isNullOrEmpty()) {
            log.error("For mange tilganger. Klarte ikke hente tilganger for bruker. response: ${response.status}")
        }

        return response.body()
    }

    @Serializable
    data class AuthorizedParty(
        val organizationNumber: String? = null,
        val type: String,
        val authorizedResources: List<String>,
        val subunits: List<AuthorizedParty>,
    )
}

enum class AltinnRessurs(val ressursId: String) {
    TILTAK_ARRANGOR_REFUSJON("tiltak-arrangor-refusjon"),
}

data class AltinnRolle(
    val organisasjonsnummer: Organisasjonsnummer,
    val ressurs: AltinnRessurs,
)
