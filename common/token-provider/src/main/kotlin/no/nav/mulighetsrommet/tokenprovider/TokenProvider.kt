package no.nav.mulighetsrommet.tokenprovider

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.builder.TokenXTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.mulighetsrommet.env.NaisEnv
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.text.ParseException
import java.util.concurrent.TimeUnit

fun interface TokenProvider {
    suspend fun exchange(accessType: AccessType): String
}

fun interface M2MTokenProvider {
    suspend fun exchange(accessType: AccessType.M2M): String
}

/**
 * Denne wrapper kall til login.microsoft i `CoroutineScope(Dispatchers.IO)`
 * som gjør at man kan gjøre token exchanges i parallel (dvs. med coroutines uten
 * å måtte brukte threads manuelt). Cachen er kun for å ikke sende N requests til
 * login.microsoft samtidig på samme key (scope + subjekt). De underliggende
 * tokenProviderene cacher selve resultatet og sjekker expiry på tokenet, derfor
 * venter vi bare på `Deffered`en, bruker ikke resultatet, men rett etterpå
 * spør igjen for å hente det cachede tokenet.
 */
class CachedTokenProvider(
    private val tokenXTokenProvider: TokenXOnBehalfOfTokenClient,
    private val oboTokenProvider: OnBehalfOfTokenClient,
    private val m2MTokenProvider: MachineToMachineTokenClient,
) {
    private val cache: Cache<String, Deferred<String>> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    companion object {
        fun init(clientId: String, tokenEndpointUrl: String): CachedTokenProvider {
            return CachedTokenProvider(
                tokenXTokenProvider = createTokenXTokenClient(clientId),
                oboTokenProvider = createOboTokenClient(clientId, tokenEndpointUrl),
                m2MTokenProvider = createM2mTokenClient(clientId, tokenEndpointUrl),
            )
        }
    }

    fun withScope(scope: String): TokenProvider {
        return TokenProvider exchange@{ accessType ->
            val key = scope + accessType.subject()

            cache.getIfPresent(key)?.await()

            val deferred = exchangeAsync(scope, accessType)
            cache.put(key, deferred)

            deferred.await()
        }
    }

    private fun exchangeAsync(scope: String, accessType: AccessType): Deferred<String> {
        return CoroutineScope(Dispatchers.IO).async {
            when (accessType) {
                is AccessType.TOKENX -> tokenXTokenProvider.exchangeOnBehalfOfToken(scope, accessType.token)
                AccessType.M2M -> m2MTokenProvider.createMachineToMachineToken(scope)
                is AccessType.OBO -> oboTokenProvider.exchangeOnBehalfOfToken(scope, accessType.token)
            }
        }
    }
}

private fun AccessType.subject(): String =
    when (this) {
        AccessType.M2M -> ""
        is AccessType.OBO -> {
            try {
                val token = JWTParser.parse(this.token)
                val subject = token.jwtClaimsSet.subject
                    ?: throw IllegalArgumentException("Unable to get subject, access token is missing subject")
                subject
            } catch (e: ParseException) {
                throw IllegalArgumentException("Unable to get subject, access token is invalid")
            }
        }

        is AccessType.TOKENX -> {
            try {
                val token = JWTParser.parse(this.token)
                val subject = token.jwtClaimsSet.subject
                    ?: throw IllegalArgumentException("Unable to get subject, access token is missing subject")
                subject
            } catch (e: ParseException) {
                throw IllegalArgumentException("Unable to get subject, access token is invalid")
            }
        }
    }

private fun createOboTokenClient(clientId: String, tokenEndpointUrl: String): OnBehalfOfTokenClient =
    when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(clientId)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(tokenEndpointUrl)
            .buildOnBehalfOfTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
    }

private fun createM2mTokenClient(clientId: String, tokenEndpointUrl: String): MachineToMachineTokenClient =
    when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(clientId)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
    }

fun createTokenXTokenClient(clientId: String): TokenXOnBehalfOfTokenClient =
    when (NaisEnv.current()) {
        NaisEnv.Local -> TokenXTokenClientBuilder.builder()
            .withClientId(clientId)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .buildOnBehalfOfTokenClient()

        else -> TokenXTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
    }

fun createMaskinportenM2mTokenClient(
    clientId: String,
    tokenEndpointUrl: String,
    issuer: String,
): MaskinPortenTokenProvider? =
    when (NaisEnv.current()) {
        NaisEnv.Local -> MaskinPortenTokenProvider(
            clientId = clientId,
            tokenEndpointUrl = tokenEndpointUrl,
            privateJwk = createMockRSAKey("maskinporten").toJSONString(),
            issuer = issuer,
        )

        NaisEnv.ProdGCP -> null // TODO: Remove when prod
        else -> MaskinPortenTokenProvider(
            clientId = clientId,
            tokenEndpointUrl = tokenEndpointUrl,
            privateJwk = System.getenv("MASKINPORTEN_CLIENT_JWK"),
            issuer = issuer,
        )
    }

private fun createMockRSAKey(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
