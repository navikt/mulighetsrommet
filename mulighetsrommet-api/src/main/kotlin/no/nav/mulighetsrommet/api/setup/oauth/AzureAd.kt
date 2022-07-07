package no.nav.mulighetsrommet.api.setup.oauth

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.setup.http.defaultHttpClient

@Serializable
data class AzureAd(
    val clientId: String,
    val clientSecret: String,
    val wellKnownConfigurationUrl: String,
    val openIdConfiguration: AzureAdOpenIdConfiguration = runBlocking {
        defaultHttpClient.get(wellKnownConfigurationUrl).body()
    }
)

@Serializable
data class AzureAdOpenIdConfiguration(
    val jwks_uri: String,
    val issuer: String,
    val token_endpoint: String,
    val authorization_endpoint: String
)
