package no.nav.mulighetsrommet.tokenprovider

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient

@Suppress("EnumEntryName")
enum class IdentityProvider {
    azuread,
    tokenx,
    maskinporten,
    idporten,
}

class TexasClient(
    private val config: Config,
    engine: HttpClientEngine,
) {
    data class Config(
        val engine: HttpClientEngine? = null,
        val tokenEndpoint: String,
        val tokenExchangeEndpoint: String,
        val tokenIntrospectionEndpoint: String,
    )

    private val client = httpJsonClient(engine).config {
        install(HttpCache)
    }

    suspend fun requestM2MToken(
        target: String,
        identityProvider: IdentityProvider,
        resource: String?,
        skipCache: Boolean?,
    ): TokenReponse {
        val response: HttpResponse = client.post(config.tokenEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                TokenRequest(
                    identity_provider = identityProvider,
                    target = target,
                    resource = resource,
                    skip_cache = skipCache,
                ),
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to fetch machine token: ${response.status}, ${response.bodyAsText()}")
        }

        return response.body<TokenReponse>()
    }

    suspend fun exchangeOBOToken(
        token: String,
        identityProvider: IdentityProvider,
        target: String,
        skipCache: Boolean?,
    ): TokenReponse {
        val response = client.post(config.tokenExchangeEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                TokenExchangeRequest(
                    user_token = token,
                    identity_provider = identityProvider,
                    target = target,
                    skip_cache = skipCache,
                ),
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to exchange token: ${response.status}, ${response.bodyAsText()}")
        }

        return response.body<TokenReponse>()
    }
}

@Serializable
data class TokenRequest(
    val identity_provider: IdentityProvider,
    // Resource indicator for audience-restricted tokens (RFC 8707).
    val resource: String?,
    // Force renewal of token. Defaults to false if omitted.
    val skip_cache: Boolean?,
    // Scope or identifier for the target application.
    val target: String,
)

@Serializable
data class TokenExchangeRequest(
    val identity_provider: IdentityProvider,
    // Force renewal of token. Defaults to false if omitted.
    val skip_cache: Boolean?,
    // Scope or identifier for the target application.
    val target: String,
    // The user's access token, usually found in the _Authorization_ header in requests to your application.
    val user_token: String,
)

@Serializable
data class TokenReponse(
    val access_token: String,
    val token_type: TokenType,
    // Token expiry in seconds. Useful for caching purposes.
    val expires_in: Long,
) {
    enum class TokenType {
        Bearer,
    }
}
