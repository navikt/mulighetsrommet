package no.nav.mulighetsrommet.api.setup.oauth

import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("issuer")
    val issuer: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String
)
