package no.nav.mulighetsrommet.tokenprovider

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.JWTBearerGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant
import java.util.*

class MaskinPortenTokenProvider(
    private val clientId: String,
    private val issuer: String,
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

    fun hentToken(scope: String, targetAudience: String): String {
        val signedJwt =
            signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(
                    clientId,
                    issuer,
                    targetAudience,
                    scope,
                ),
                assertionSigner,
            )

        val request =
            TokenRequest(
                tokenEndpoint,
                JWTBearerGrant(signedJwt),
                Scope(*(scope.split(" ")).toTypedArray()),
            )

        val response = TokenResponse.parse(request.toHTTPRequest().send())

        if (!response.indicatesSuccess()) {
            val tokenErrorResponse = response.toErrorResponse()

            log.error("Failed to fetch maskinporten token. Error: {}", tokenErrorResponse.toJSONObject().toString())

            throw RuntimeException("Failed to fetch maskinporten token")
        }

        val successResponse = response.toSuccessResponse()

        val accessToken = successResponse.tokens.accessToken

        return accessToken.value
    }

    private fun signedClientAssertion(
        assertionHeader: JWSHeader,
        assertionClaims: JWTClaimsSet,
        signer: JWSSigner,
    ): SignedJWT {
        val signedJWT = SignedJWT(assertionHeader, assertionClaims)
        signedJWT.sign(signer)
        return signedJWT
    }

    private fun clientAssertionHeader(keyId: String): JWSHeader {
        val headerClaims: MutableMap<String, Any> = HashMap()
        headerClaims["kid"] = keyId
        headerClaims["typ"] = "JWT"
        headerClaims["alg"] = "RS256"
        return JWSHeader.parse(headerClaims)
    }

    private fun clientAssertionClaims(
        clientId: String,
        issuer: String,
        targetAudience: String,
        scope: String,
    ): JWTClaimsSet {
        val now = Instant.now()
        val expire = now.plusSeconds(30)

        return JWTClaimsSet
            .Builder()
            .subject(clientId)
            .audience(issuer)
            .issuer(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expire))
            .notBeforeTime(Date.from(now))
            .claim("scope", scope)
            .claim("resource", targetAudience)
            .jwtID(UUID.randomUUID().toString())
            .build()
    }

    fun withScope(scope: String, targetAudience: String): M2MTokenProvider {
        return M2MTokenProvider exchange@{ accessType ->
            hentToken(scope, targetAudience)
        }
    }
}
