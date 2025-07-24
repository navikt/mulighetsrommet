package no.nav.mulighetsrommet.tokenprovider

fun interface TokenProvider {
    suspend fun exchange(accessType: AccessType): String
}

fun interface M2MTokenProvider {
    suspend fun exchange(accessType: AccessType.M2M): String
}

class AzureAdTokenProvider(private val texasClient: TexasClient) {
    fun withScope(scope: String): TokenProvider {
        return TokenProvider exchange@{ accessType ->
            when (accessType) {
                AccessType.M2M -> texasClient.requestM2MToken(
                    target = scope,
                    IdentityProvider.azuread,
                    resource = null,
                    skipCache = false,
                )
                is AccessType.OBO -> texasClient.exchangeOBOToken(
                    token = accessType.token,
                    identityProvider = IdentityProvider.azuread,
                    target = scope,
                    skipCache = false,
                )
            }.access_token
        }
    }
}

class MaskinportenTokenProvider(private val texasClient: TexasClient) {
    fun withScopeAndResource(
        scope: String,
        resource: String,
    ): M2MTokenProvider {
        return M2MTokenProvider exchange@{ _ ->
            texasClient.requestM2MToken(
                target = scope,
                identityProvider = IdentityProvider.maskinporten,
                resource = resource,
                skipCache = false,
            ).access_token
        }
    }
}
