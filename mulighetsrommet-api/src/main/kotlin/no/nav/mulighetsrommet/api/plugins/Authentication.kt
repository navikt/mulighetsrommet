package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.AuthConfig
import no.nav.mulighetsrommet.ktor.exception.StatusException
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    AzureAdNavIdent,
    AzureAdDefaultApp,
    AzureAdTiltaksgjennomforingApp,
}

object AppRoles {
    const val AccessAsApplication = "access_as_application"
    const val ReadTiltaksgjennomforing = "tiltaksgjennomforing-read"
}

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    fun hasRoles(credentials: JWTCredential, vararg requiredRoles: String): Boolean {
        val roles = credentials.getListClaim("roles", String::class)
        return requiredRoles.all { it in roles }
    }

    install(Authentication) {
        jwt(AuthProvider.AzureAdNavIdent.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: return@validate null

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AzureAdDefaultApp.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                if (!hasRoles(credentials, AppRoles.AccessAsApplication)) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AzureAdTiltaksgjennomforingApp.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                if (!hasRoles(credentials, AppRoles.AccessAsApplication, AppRoles.ReadTiltaksgjennomforing)) {
                    return@validate null
                }

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

/**
 * Gets a NavAnsattAzureId from the underlying [JWTPrincipal], or throws a [StatusException]
 * if the claim is not available.
 */
fun <T : Any> PipelineContext<T, ApplicationCall>.getNavAnsattAzureId(): UUID {
    return call.principal<JWTPrincipal>()
        ?.get("oid")
        ?.let { UUID.fromString(it) }
        ?: throw StatusException(HttpStatusCode.Forbidden, "NavAnsattAzureId mangler i JWTPrincipal")
}
