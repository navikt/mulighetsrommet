package no.nav.mulighetsrommet.tokenprovider

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient

enum class IdentityProvider(val value: String) {
    AZURE_AD("azuread"),
    TOKENX("tokenx"),
    MASKINPORTEN("maskinporten"),
    IDPORTEN("idporten"),
}

class TexasClient(
    private val config: Config,
    clientEngine: HttpClientEngine,
) {
    data class Config(
        val tokenEndpoint: String,
        val tokenExchangeEndpoint: String,
        val tokenIntrospectionEndpoint: String,
    )

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun requestMachineToken(
        target: String,
        identityProvider: IdentityProvider,
        resource: String?,
    ): String {
        val formParams = buildString {
            append("target=$target&identity_provider=${identityProvider.value}")
            if (!resource.isNullOrEmpty()) {
                append("&resource=$resource")
            }
        }

        val response: HttpResponse = client.post(config.tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(formParams)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to fetch machine token: ${response.status}, ${response.bodyAsText()}")
        }

        val responseBody = response.body<TokenReponse>()
        return responseBody.access_token
    }

    suspend fun exchangeToken(token: String): String {
        val response = client.post(config.tokenExchangeEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("token=$token")
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to exchange token: ${response.status}, ${response.bodyAsText()}")
        }

        val responseBody = response.body<TokenReponse>()
        return responseBody.access_token
    }
}

@Serializable
data class TokenReponse(
    val access_token: String,
    val token_type: TokenType,
    val expires_in: Long,
) {
    enum class TokenType {
        Bearer,
    }
}
