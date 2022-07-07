package no.nav.mulighetsrommet.api.utils

import com.github.michaelbull.result.get
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.setup.oauth.AzureAdClient

class TokenProviders(
    val azureAdClient: AzureAdClient,
    val config: AppConfig
) {

    val veilarbvedtaksstotteTokenProvider: suspend (String?) -> String? = { accessToken ->
         accessToken?.let {
             azureAdClient.getOnBehalfOfAccessTokenForResource(
                 scopes = listOf(config.veilarbvedtaksstotteConfig.authenticationScope),
                 accessToken = it
             ).get()?.accessToken
         }
     }

    val veilarboppfolgingTokenProvider: suspend (String?) -> String? = { accessToken ->
        accessToken?.let {
            azureAdClient.getOnBehalfOfAccessTokenForResource(
                scopes = listOf(config.veilarboppfolgingConfig.authenticationScope),
                accessToken = it
            ).get()?.accessToken
        }
    }
}
