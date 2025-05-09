package no.nav.mulighetsrommet.api.clients.msgraph

import arrow.core.Either
import arrow.core.getOrElse
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

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
    private val tokenProvider: TokenProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ansattDataCache: Cache<UUID, AzureAdNavAnsatt> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(2000)
        .recordStats()
        .build()

    private val ansattDataCacheByNavIdent: Cache<NavIdent, AzureAdNavAnsatt> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(2000)
        .recordStats()
        .build()

    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5)
            exponentialDelay()
        }
    }

    private val azureAdNavAnsattFields =
        "id,streetAddress,city,givenName,surname,onPremisesSamAccountName,mail,mobilePhone"

    suspend fun getNavAnsatt(navAnsattOid: UUID, accessType: AccessType): AzureAdNavAnsatt {
        return CacheUtils.tryCacheFirstNotNull(ansattDataCache, navAnsattOid) {
            val response = client.get("$baseUrl/v1.0/users/$navAnsattOid") {
                bearerAuth(tokenProvider.exchange(accessType))
                parameter($$"$select", azureAdNavAnsattFields)
            }

            if (!response.status.isSuccess()) {
                logAndThrowError("Klarte ikke finne bruker med id=$navAnsattOid", response)
            }

            val user = response.body<MsGraphUserDto>()

            toNavAnsatt(user).getOrElse { throw it }
        }
    }

    suspend fun getNavAnsattByNavIdent(navIdent: NavIdent, accessType: AccessType): AzureAdNavAnsatt? {
        return CacheUtils.tryCacheFirstNullable(ansattDataCacheByNavIdent, navIdent) {
            val response = client.get("$baseUrl/v1.0/users") {
                bearerAuth(tokenProvider.exchange(accessType))
                parameter($$"$search", "\"onPremisesSamAccountName:${navIdent.value}\"")
                parameter($$"$select", azureAdNavAnsattFields)
                header("ConsistencyLevel", "eventual")
            }

            if (!response.status.isSuccess()) {
                logAndThrowError("Feil ved søk på bruker med navIdent=$navIdent", response)
            }

            response.body<GetUserSearchResponse>()
                .value
                .firstOrNull()
                ?.let {
                    toNavAnsatt(it).getOrElse { ex -> throw ex }
                }
        }
    }

    suspend fun getNavAnsattSok(nameQuery: String): List<AzureAdNavAnsatt> {
        if (nameQuery.isBlank()) {
            return emptyList()
        }
        val response = client.get("$baseUrl/v1.0/users") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            parameter($$"$search", "\"displayName:$nameQuery\"")
            parameter($$"$orderBy", "displayName")
            parameter($$"$select", azureAdNavAnsattFields)
            header("ConsistencyLevel", "eventual")
        }

        if (!response.status.isSuccess()) {
            logAndThrowError("Feil under user search mot Azure", response)
        }

        return response.body<GetUserSearchResponse>()
            .value
            // Her returneres mye rart (f. eks ikke personer) så vi filtrerer vekk de som mangler required felter
            .mapNotNull { toNavAnsatt(it).getOrNull() }
    }

    suspend fun checkMemberGroups(navAnsattOid: UUID, groupIds: List<UUID>, accessType: AccessType): List<UUID> {
        if (groupIds.isEmpty()) {
            return emptyList()
        }

        val response = client.post("$baseUrl/v1.0/users/$navAnsattOid/checkMemberGroups") {
            bearerAuth(tokenProvider.exchange(accessType))
            contentType(ContentType.Application.Json)
            setBody(CheckMemberGroupsRequest(groupIds))
        }

        if (!response.status.isSuccess()) {
            logAndThrowError("Klarte ikke sjekke gruppemedlemskap for bruker id=$navAnsattOid", response)
        }

        val result = response.body<CheckMemberGroupsResponse>()

        return result.value.map { it }
    }

    suspend fun getGroupMembers(groupId: UUID): List<AzureAdNavAnsatt> {
        val response = client.get("$baseUrl/v1.0/groups/$groupId/members") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            parameter($$"$select", azureAdNavAnsattFields)
            parameter($$"$top", "999")
        }

        if (!response.status.isSuccess()) {
            logAndThrowError("Klarte ikke hente medlemmer i AD-gruppe med id=$groupId", response)
        }

        val result = response.body<GetGroupMembersResponse>()

        return result.value
            .filter { isNavAnsatt(it) }
            .map { toNavAnsatt(it).getOrElse { ex -> throw ex } }
    }

    suspend fun addToGroup(objectId: UUID, groupId: UUID) {
        val response = client.post($$"$$baseUrl/v1.0/groups/$$groupId/members/$ref") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            setBody(AddMemberRequest("https://graph.microsoft.com/v1.0/directoryObjects/$objectId"))
        }

        if (!response.status.isSuccess()) {
            logAndThrowError("Klarte ikke legge til medlem i AD-gruppe med id=$groupId", response)
        }
    }

    /**
     * Når NAVident er definert på en MsGraphUserDto så anser vi brukeren som en Nav-ansatt.
     */
    private fun isNavAnsatt(it: MsGraphUserDto) = it.onPremisesSamAccountName != null

    private fun toNavAnsatt(user: MsGraphUserDto): Either<Throwable, AzureAdNavAnsatt> = Either.catch {
        when {
            user.onPremisesSamAccountName == null -> {
                throw IllegalArgumentException("NAVident mangler for bruker med id=${user.id}")
            }

            user.streetAddress == null -> {
                throw IllegalArgumentException("Nav Enhetskode mangler for bruker med id=${user.id}")
            }

            user.city == null -> {
                throw IllegalArgumentException("Nav Enhetsnavn mangler for bruker med id=${user.id}")
            }

            user.givenName == null -> {
                throw IllegalArgumentException("Fornavn på ansatt mangler for bruker med id=${user.id}")
            }

            user.surname == null -> {
                throw IllegalArgumentException("Etternavn på ansatt mangler for bruker med id=${user.id}")
            }

            user.mail == null -> {
                throw IllegalArgumentException("Epost på ansatt mangler for bruker med id=${user.id}")
            }

            else -> AzureAdNavAnsatt(
                azureId = user.id,
                navIdent = NavIdent(user.onPremisesSamAccountName),
                fornavn = user.givenName,
                etternavn = user.surname,
                hovedenhetKode = NavEnhetNummer(user.streetAddress),
                hovedenhetNavn = user.city,
                mobilnummer = user.mobilePhone,
                epost = user.mail,
            )
        }
    }

    private suspend fun logAndThrowError(message: String, response: HttpResponse): Nothing {
        log.error("Message=$message Status=${response.status} Body=${response.bodyAsText()}")
        throw RuntimeException(message)
    }
}

@Serializable
internal data class AddMemberRequest(
    @SerialName("@odata.id")
    val odataId: String,
)

@Serializable
internal data class CheckMemberGroupsRequest(
    val groupIds: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)

@Serializable
internal data class CheckMemberGroupsResponse(
    val value: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
