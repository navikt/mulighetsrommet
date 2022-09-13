package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.AuthConfig
import no.nav.mulighetsrommet.ktor.exception.StatusException
import java.net.URI
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    AzureAdMachineToMachine,
    AzureAdNavIdent,
}

fun Application.configureAuthentication(auth: AuthConfig) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt(AuthProvider.AzureAdMachineToMachine.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                // TODO: verify that this is a m2m-token

                JWTPrincipal(credentials.payload)
            }
        }
        jwt(AuthProvider.AzureAdNavIdent.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: return@validate null

                JWTPrincipal(credentials.payload)
            }
        }
    }
}

/**
 * Gets a NAVident from the underlying [JWTPrincipal], or throws a [StatusException]
 * if the claim is not available.
 */
fun <T : Any> PipelineContext<T, ApplicationCall>.getNavIdent(): String {
    return call.principal<JWTPrincipal>()
        ?.get("NAVident")
        ?: throw StatusException(HttpStatusCode.Forbidden, "NAVident mangler i JWTPrincipal")
}
