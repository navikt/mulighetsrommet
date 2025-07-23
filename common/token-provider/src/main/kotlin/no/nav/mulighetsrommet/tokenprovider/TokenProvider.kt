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
                AccessType.M2M -> texasClient.requestMachineToken(target = scope, IdentityProvider.AZURE_AD, null)
                is AccessType.OBO -> texasClient.exchangeToken(token = accessType.token)
            }
        }
    }
}

class MaskinportenTokenProvider(private val texasClient: TexasClient) {
    fun withScopeAndResource(
        scope: String,
        resource: String,
    ): M2MTokenProvider {
        return M2MTokenProvider exchange@{ _ ->
            texasClient.requestMachineToken(
                target = scope,
                IdentityProvider.MASKINPORTEN,
                resource,
            )
        }
    }
}
