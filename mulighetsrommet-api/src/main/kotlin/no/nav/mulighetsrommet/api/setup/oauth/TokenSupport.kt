package no.nav.mulighetsrommet.api.setup.oauth

import no.nav.mulighetsrommet.api.setup.Cluster
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig

fun azureAdtokenSupportConfig(azureAd: AzureAd): TokenSupportConfig {
    val issuerConfig = IssuerConfig(
        name = "azuread",
        discoveryUrl = azureAd.wellKnownConfigurationUrl,
        acceptedAudience = listOf(
            azureAd.clientId,
            "api://${Cluster.current.asString()}.team-mulighetsrommet.mulighetsrommet-api/.default"
        )
    )
    return TokenSupportConfig(issuerConfig)
}
