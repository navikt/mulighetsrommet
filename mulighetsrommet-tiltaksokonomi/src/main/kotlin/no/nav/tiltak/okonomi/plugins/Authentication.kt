package no.nav.tiltak.okonomi.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import no.nav.tiltak.okonomi.AuthConfig
import java.net.URI
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    AZURE_AD_OEBS_API,
}

object AppRoles {
    const val ACCESS_AS_APPLICATION = "access_as_application"
    const val OEBS_API = "oebs_api"
}

/**
 * Utility to implement a JWT [Authentication] provider with its named derived from the [authProvider] paramater.
 */
private fun AuthenticationConfig.jwt(
    authProvider: AuthProvider,
    configure: JWTAuthenticationProvider.Config.() -> Unit,
) = jwt(authProvider.name, configure)

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    fun hasApplicationRoles(credentials: JWTCredential, vararg requiredRoles: String): Boolean {
        val roles = credentials.getListClaim("roles", String::class)
        return requiredRoles.all { it in roles }
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_OEBS_API) {
            verifier(jwkProvider, auth.azure.issuer) {
                withAudience(auth.azure.audience)
            }

            validate { credentials ->
                if (!hasApplicationRoles(
                        credentials,
                        AppRoles.ACCESS_AS_APPLICATION,
                        AppRoles.OEBS_API,
                    )
                ) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }
    }
}
