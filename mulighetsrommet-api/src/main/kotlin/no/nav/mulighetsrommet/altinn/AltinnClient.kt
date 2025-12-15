package no.nav.mulighetsrommet.altinn

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.either
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
    private val tokenProvider: M2MTokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun hentRettigheter(norskIdent: NorskIdent): Either<AltinnError, List<BedriftRettigheter>> {
        return hentAuthorizedParties(norskIdent)
            .map { findAltinnRoller(it) }
    }

    private fun findAltinnRoller(
        parties: List<AuthorizedParty>,
    ): List<BedriftRettigheter> = parties.filter { it.type == AuthorizedPartyType.Organization }
        .map {
            AuthorizedOrganization(
                organizationNumber = requireNotNull(it.organizationNumber) { "Organisasjonsnummer mangler på type Organization" },
                organizationName = it.name,
                authorizedResources = it.authorizedResources,
                subunits = it.subunits,
            )
        }
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

    private suspend fun hentAuthorizedParties(norskIdent: NorskIdent): Either<AltinnError, List<AuthorizedParty>> = either {
        val response = client.post("$baseUrl/accessmanagement/api/v1/resourceowner/authorizedparties") {
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
            return AltinnError.Error.left()
        }

        if (!response.headers["X-Warning-LimitReached"].isNullOrEmpty()) {
            log.error("For mange tilganger. Klarte ikke hente tilganger for bruker. response: ${response.status}")
            return AltinnError.ForMangeTilganger.left()
        }

        response.body()
    }

    @Serializable
    data class AuthorizedParty(
        val organizationNumber: String?,
        val name: String,
        val type: AuthorizedPartyType,
        val authorizedResources: List<String>,
        val subunits: List<AuthorizedParty>,
    )

    @Serializable
    data class AuthorizedOrganization(
        val organizationNumber: String,
        val organizationName: String,
        val authorizedResources: List<String>,
        val subunits: List<AuthorizedParty>,
    )

    enum class AuthorizedPartyType {
        Person,
        Organization,
    }

    @Serializable
    data class AltinnRequest(
        val type: String,
        val value: String,
    )
}

enum class AltinnError {
    Error,
    ForMangeTilganger,
}
