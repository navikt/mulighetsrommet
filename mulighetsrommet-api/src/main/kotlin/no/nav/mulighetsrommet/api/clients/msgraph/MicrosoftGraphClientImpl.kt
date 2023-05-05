package no.nav.mulighetsrommet.api.clients.msgraph

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Graph explorer:
 * - https://developer.microsoft.com/en-us/graph/graph-explorer
 */
class MicrosoftGraphClientImpl(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
) : MicrosoftGraphClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(engine).config {
        install(HttpCache)

        defaultRequest {
            url(baseUrl)
        }
    }

    override suspend fun getNavAnsatt(accessToken: String, navAnsattAzureId: UUID): NavAnsattDto {
        val response = client.get("/v1.0/users/$navAnsattAzureId") {
            bearerAuth(tokenProvider(accessToken))
            parameter("\$select", "streetAddress,city,givenName,surname,onPremisesSamAccountName")
        }

        if ((response.status == HttpStatusCode.NotFound) || (response.status == HttpStatusCode.NoContent)) {
            SecureLog.logger.warn("Klarte ikke finne bruker med azure-id: $navAnsattAzureId")
            log.error("Klarte ikke finne bruker med azure-id. Se detaljer i secureLog.")
            throw RuntimeException("Klarte ikke finne bruker med azure-id. Finnes brukeren i AD?")
        }

        val user = response.body<MsGraphUserDto>()
        return NavAnsattDto(
            hovedenhetKode = user.streetAddress,
            hovedenhetNavn = user.city,
            fornavn = user.givenName,
            etternavn = user.surname,
            navident = user.onPremisesSamAccountName,
        )
    }

    override suspend fun getGroupMembers(groupId: UUID): List<NavAnsattDto> {
        val response = client.get("$baseUrl/v1.0/groups/$groupId/members") {
            parameter("\$select", "streetAddress,city,givenName,surname,onPremisesSamAccountName")
        }

        val result = response.body<ODataListResponse<MsGraphUserDto>>()

        return result.value.map { user ->
            NavAnsattDto(
                hovedenhetKode = user.streetAddress,
                hovedenhetNavn = user.city,
                fornavn = user.givenName,
                etternavn = user.surname,
                navident = user.onPremisesSamAccountName,
            )
        }
    }
}
