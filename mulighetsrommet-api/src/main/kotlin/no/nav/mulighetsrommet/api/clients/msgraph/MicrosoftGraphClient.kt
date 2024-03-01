package no.nav.mulighetsrommet.api.clients.msgraph

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Graph explorer kan benyttes for å utforske API'et til msgraph:
 * - https://developer.microsoft.com/en-us/graph/graph-explorer
 *
 * For å få tilgang til nye endepunkter/datafelter så må man spørre om spesifikke tilganger i #tech-azure.
 *
 * Vi har eksplisitt spurt om, og fått tildelt, følgende tilganger:
 * - Claim: User.Read.All, Type: Application
 * - Claim: GroupMember.Read.All, Type: Application
 */
class MicrosoftGraphClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String?) -> String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5)
            exponentialDelay()
        }
    }

    suspend fun getNavAnsatt(navAnsattAzureId: UUID, oboToken: String?): AzureAdNavAnsatt {
        val response = client.get("$baseUrl/v1.0/users/$navAnsattAzureId") {
            bearerAuth(tokenProvider(oboToken))
            parameter("\$select", "id,streetAddress,city,givenName,surname,onPremisesSamAccountName,mail,mobilePhone")
        }

        if (!response.status.isSuccess()) {
            log.error("Klarte ikke finne bruker med id=$navAnsattAzureId")
            throw RuntimeException("Klarte ikke finne bruker med id=$navAnsattAzureId. Finnes brukeren i AD?")
        }

        val user = response.body<MsGraphUserDto>()

        return toNavAnsatt(user)
    }

    suspend fun getMemberGroups(navAnsattAzureId: UUID, oboToken: String?): List<AdGruppe> {
        val response = client.get("$baseUrl/v1.0/users/$navAnsattAzureId/transitiveMemberOf/microsoft.graph.group") {
            bearerAuth(tokenProvider.invoke(oboToken))
            parameter("\$select", "id,displayName")
        }

        if (!response.status.isSuccess()) {
            log.error("Klarte ikke hente AD-grupper for bruker id=$navAnsattAzureId")
            throw RuntimeException("Klarte ikke hente AD-grupper for bruker id=$navAnsattAzureId")
        }

        val result = response.body<GetMemberGroupsResponse>()

        return result.value.map { group ->
            AdGruppe(id = group.id, navn = group.displayName)
        }
    }

    suspend fun getGroupMembers(groupId: UUID): List<AzureAdNavAnsatt> {
        val response = client.get("$baseUrl/v1.0/groups/$groupId/members") {
            bearerAuth(tokenProvider.invoke(null))
            parameter("\$select", "id,streetAddress,city,givenName,surname,onPremisesSamAccountName,mail,mobilePhone")
            parameter("\$top", "999")
        }

        if (!response.status.isSuccess()) {
            log.error("Klarte ikke hente medlemmer i AD-gruppe med id=$groupId: {}", response.bodyAsText())
            throw RuntimeException("Klarte ikke hente medlemmer i AD-gruppe med id=$groupId")
        }

        val result = response.body<GetGroupMembersResponse>()

        return result.value
            .filter { isNavAnsatt(it) }
            .map { toNavAnsatt(it) }
    }

    /**
     * Når NAVident er definert på en MsGraphUserDto så anser vi brukeren som en NAV-ansatt.
     */
    private fun isNavAnsatt(it: MsGraphUserDto) = it.onPremisesSamAccountName != null

    private fun toNavAnsatt(user: MsGraphUserDto) = when {
        user.onPremisesSamAccountName == null -> {
            throw RuntimeException("NAVident mangler for bruker med id=${user.id}")
        }

        user.streetAddress == null -> {
            throw RuntimeException("NAV Enhetskode mangler for bruker med id=${user.id}")
        }

        user.city == null -> {
            throw RuntimeException("NAV Enhetsnavn mangler for bruker med id=${user.id}")
        }

        user.givenName == null -> {
            throw RuntimeException("Fornavn på ansatt mangler for bruker med id=${user.id}")
        }

        user.surname == null -> {
            throw RuntimeException("Etternavn på ansatt mangler for bruker med id=${user.id}")
        }

        else -> AzureAdNavAnsatt(
            azureId = user.id,
            navIdent = NavIdent(user.onPremisesSamAccountName),
            fornavn = user.givenName,
            etternavn = user.surname,
            hovedenhetKode = user.streetAddress,
            hovedenhetNavn = user.city,
            mobilnummer = user.mobilePhone,
            epost = user.mail,
        )
    }
}
