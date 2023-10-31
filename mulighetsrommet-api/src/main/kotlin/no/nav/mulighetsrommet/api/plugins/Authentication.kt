package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.AuthConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.ktor.exception.StatusException
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    AZURE_AD_NAV_IDENT,
    AZURE_AD_TEAM_MULIGHETSROMMET,
    AZURE_AD_DEFAULT_APP,
    AZURE_AD_TILTAKSGJENNOMFORING_APP,
    AZURE_AD_ADMIN_FLATE_TILGANG,
}

object AppRoles {
    const val ACCESS_AS_APPLICATION = "access_as_application"
    const val READ_TILTAKSGJENNOMFORING = "tiltaksgjennomforing-read"
}

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    fun ApplicationCall.hasApplicationRoles(credentials: JWTCredential, vararg requiredRoles: String): Boolean {
        val roles = credentials.getListClaim("roles", String::class)
        val hasRequiredRoles = requiredRoles.all { it in roles }
        if (!hasRequiredRoles) {
            application.log.warn("Access denied. Mangler en av rollene '$requiredRoles'.")
        }
        return hasRequiredRoles
    }

    fun ApplicationCall.hasNavAnsattRoles(credentials: JWTCredential, vararg requiredRoles: NavAnsattRolle): Boolean {
        val navAnsattGroups = credentials.getListClaim("groups", UUID::class)
        val hasRequiredRoles = requiredRoles.all { requiredRole ->
            auth.roles.any { (groupId, role) -> role == requiredRole && groupId in navAnsattGroups }
        }
        if (!hasRequiredRoles) {
            application.log.warn("Mangler en av rollene '$requiredRoles'.")
        }
        return hasRequiredRoles
    }

    install(Authentication) {
        jwt(AuthProvider.AZURE_AD_TEAM_MULIGHETSROMMET.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: run {
                    application.log.warn("Access denied. Mangler claim 'NAVident'.")
                    return@validate null
                }

                if (!hasNavAnsattRoles(credentials, NavAnsattRolle.TEAM_MULIGHETSROMMET)) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_ADMIN_FLATE_TILGANG.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: run {
                    application.log.warn("Access denied. Mangler claim 'NAVident'.")
                    return@validate null
                }

                if (!hasNavAnsattRoles(credentials, NavAnsattRolle.TEAM_MULIGHETSROMMET) &&
                    !hasNavAnsattRoles(credentials, NavAnsattRolle.BETABRUKER)
                ) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_NAV_IDENT.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: run {
                    application.log.warn("Access denied. Mangler claim 'NAVident'.")
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_DEFAULT_APP.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                if (!hasApplicationRoles(credentials, AppRoles.ACCESS_AS_APPLICATION)) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP.name) {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                if (!hasApplicationRoles(
                        credentials,
                        AppRoles.ACCESS_AS_APPLICATION,
                        AppRoles.READ_TILTAKSGJENNOMFORING,
                    )
                ) {
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
