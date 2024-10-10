package no.nav.mulighetsrommet.tokenprovider

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import no.nav.common.token_client.utils.TokenClientUtils
import org.slf4j.LoggerFactory
import java.net.URI

class MaskinPortenTokenProvider(
    private val clientId: String,
    tokenEndpointUrl: String,
    privateJwk: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val tokenEndpoint: URI
    private val privateJwkKeyId: String
    private val assertionSigner: JWSSigner

    data class Config(
        val clientId: String,
        val issuer: String,
        val tokenEndpointUrl: String,
        val privateJwk: String,
    )

    init {
        val rsaKey = RSAKey.parse(privateJwk)

        tokenEndpoint = URI.create(tokenEndpointUrl)
        privateJwkKeyId = rsaKey.keyID
        assertionSigner = RSASSASigner(rsaKey)
    }

    private fun createToken(scope: String, targetAudience: String): String {
        val signedJwt = TokenClientUtils.signedClientAssertion(
            TokenClientUtils.clientAssertionHeader(privateJwkKeyId),
            TokenClientUtils.clientAssertionClaims(clientId, tokenEndpoint.toString()),
            assertionSigner,
        )

        val request = TokenRequest(
            tokenEndpoint,
            signedJwt,
            ClientCredentialsGrant(),
            Scope(scope),
            null,
            mapOf("resource" to listOf(targetAudience)),
        )

        val response = TokenResponse.parse(request.toHTTPRequest().send())

        if (!response.indicatesSuccess()) {
            val tokenErrorResponse = response.toErrorResponse()
            log.error(
                "Failed to fetch Maskinporten M2M token for scope={}. Error: {}",
                scope,
                tokenErrorResponse.toJSONObject().toString(),
            )
            throw RuntimeException("Failed to fetch Maskinporten M2M token for scope=$scope")
        }

        val successResponse = response.toSuccessResponse()

        val accessToken = successResponse.tokens.accessToken

        return accessToken.value
    }

    fun withScope(scope: String, targetAudience: String): M2MTokenProvider {
        return M2MTokenProvider exchange@{ accessType ->
            createToken(scope, targetAudience)
        }
    }
}
