package no.nav.mulighetsrommet.tokenprovider

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.util.*

class MaskinPortenTokenProvider(
    private val clientId: String,
    private val issuer: String,
    private val tokenEndpointUrl: String,
    private val privateJwk: String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    data class Config(
        val clientId: String,
        val issuer: String,
        val tokenEndpointUrl: String,
        val privateJwk: String,
    )

    suspend fun createToken(scope: String, targetAudience: String): String {
        val signedJwt = signedJWT(scope, targetAudience)

        val response = client.post(tokenEndpointUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("assertion", signedJwt.serialize())
                        append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                        append("client_assertion", signedJwt.serialize())
                        append("scope", scope)
                    },
                ),
            )
        }

        if (response.status != HttpStatusCode.OK) {
            log.error(
                "Failed to fetch Maskinporten M2M token for scope={}. Status: {}, Error: {}",
                scope,
                response.status,
                response.bodyAsText(),
            )
            throw RuntimeException("Failed to fetch Maskinporten M2M token for scope=$scope")
        }

        return response
            .body<AccessTokenResponse>()
            .accessToken
    }

    fun withScope(scope: String, targetAudience: String): M2MTokenProvider {
        return M2MTokenProvider exchange@{ accessType ->
            createToken(scope, targetAudience)
        }
    }

    private fun signedJWT(scope: String, targetAudience: String): SignedJWT {
        val rsaKey = RSAKey.parse(privateJwk)
        val signer = RSASSASigner(rsaKey.toPrivateKey())

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT)
            .build()

        val now = Date()
        val claims: JWTClaimsSet = JWTClaimsSet.Builder()
            .subject(clientId)
            .issuer(clientId)
            .audience(issuer)
            .issueTime(now)
            .notBeforeTime(now)
            .claim("scope", scope)
            .claim("resource", targetAudience)
            .expirationTime(Date(now.toInstant().plusSeconds(30).toEpochMilli()))
            .jwtID(UUID.randomUUID().toString())
            .build()

        return SignedJWT(header, claims)
            .apply { sign(signer) }
    }
}

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
)
