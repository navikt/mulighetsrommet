package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.api.AuthConfig
import no.nav.mulighetsrommet.api.navansatt.NavAnsattPrincipalService
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.koin.ktor.ext.inject
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    AZURE_AD_NAV_IDENT,
    AZURE_AD_DEFAULT_APP,
    AZURE_AD_TILTAKSGJENNOMFORING_APP,
    NAV_ANSATT_WITH_ROLES,
    TOKEN_X_ARRANGOR_FLATE,
}

object AppRoles {
    const val ACCESS_AS_APPLICATION = "access_as_application"
    const val READ_TILTAKSGJENNOMFORING = "tiltaksgjennomforing-read"
}

/**
 * Utility that requires all [AuthProvider]'s specified in [configurations] to authenticate the request.
 */
fun Route.authenticate(
    vararg configurations: AuthProvider,
    build: Route.() -> Unit,
): Route {
    return authenticate(
        configurations = configurations.map { it.name }.toTypedArray(),
        strategy = AuthenticationStrategy.Required,
        build = build,
    )
}

/**
 * Gets a NAVident from the underlying [JWTPrincipal], or throws a [StatusException]
 * if the claim is not available.
 */
fun RoutingContext.getNavIdent(): NavIdent {
    return call.principal<JWTPayloadHolder>()?.get("NAVident")?.let { NavIdent(it) } ?: throw StatusException(
        HttpStatusCode.Forbidden,
        "NAVident mangler i JWTPrincipal",
    )
}

/**
 * Gets a NavAnsattAzureId from the underlying [JWTPrincipal], or throws a [StatusException]
 * if the claim is not available.
 */
fun RoutingContext.getNavAnsattAzureId(): UUID {
    return call.principal<JWTPayloadHolder>()?.get("oid")?.let { UUID.fromString(it) } ?: throw StatusException(
        HttpStatusCode.Forbidden,
        "NavAnsattAzureId mangler i JWTPrincipal",
    )
}

/**
 * Gets a pid from the underlying [JWTPrincipal], or throws a [StatusException]
 * if the claim is not available.
 */
fun RoutingContext.getPid(): NorskIdent {
    return call.principal<JWTPayloadHolder>()?.get("pid")?.let { NorskIdent(it) } ?: throw StatusException(
        HttpStatusCode.Forbidden,
        "pid mangler i JWTPrincipal",
    )
}

/**
 * Utility to implement a JWT [Authentication] provider with its named derived from the [authProvider] parameter.
 */
private fun AuthenticationConfig.jwt(
    authProvider: AuthProvider,
    configure: JWTAuthenticationProvider.Config.() -> Unit,
) = jwt(authProvider.name, configure)

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val azureJwkProvider = JwkProviderBuilder(URI(auth.azure.jwksUri).toURL()).cached(5, 12, TimeUnit.HOURS).build()
    val tokenxJwkProvider = JwkProviderBuilder(URI(auth.tokenx.jwksUri).toURL()).cached(5, 12, TimeUnit.HOURS).build()

    val altinnRettigheterService: AltinnRettigheterService by inject()

    val navAnsattPrincipalService: NavAnsattPrincipalService by inject()

    fun hasApplicationRoles(credentials: JWTCredential, vararg requiredRoles: String): Boolean {
        val roles = credentials.getListClaim("roles", String::class)
        return requiredRoles.all { it in roles }
    }

    install(Authentication) {
        jwt(AuthProvider.NAV_ANSATT_WITH_ROLES) {
            verifier(azureJwkProvider, auth.azure.issuer) {
                withAudience(auth.azure.audience)
            }

            validate { credentials ->
                navAnsattPrincipalService.resolveNavAnsattPrincipal(credentials)?.takeIf { it.roller.isNotEmpty() }
            }
        }

        jwt(AuthProvider.AZURE_AD_NAV_IDENT) {
            verifier(azureJwkProvider, auth.azure.issuer) {
                withAudience(auth.azure.audience)
            }

            validate { credentials ->
                credentials["NAVident"] ?: return@validate null

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_DEFAULT_APP) {
            verifier(azureJwkProvider, auth.azure.issuer) {
                withAudience(auth.azure.audience)
            }

            validate { credentials ->
                if (!hasApplicationRoles(credentials, AppRoles.ACCESS_AS_APPLICATION)) {
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }

        jwt(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP) {
            verifier(azureJwkProvider, auth.azure.issuer) {
                withAudience(auth.azure.audience)
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

        jwt(AuthProvider.TOKEN_X_ARRANGOR_FLATE) {
            verifier(tokenxJwkProvider, auth.tokenx.issuer) {
                withAudience(auth.tokenx.audience)
            }

            validate { credentials ->
                val pid = credentials["pid"] ?: run {
                    application.log.warn("'pid' claim is missing from token")
                    return@validate null
                }

                val norskIdent = runCatching { NorskIdent(pid) }
                    .onFailure { application.log.warn("Failed to parse 'pid' claim as NorskIdent") }
                    .getOrElse { return@validate null }

                val organisasjonsnummer = altinnRettigheterService.getRettigheter(norskIdent)
                    .filter { AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING in it.rettigheter }
                    .map { it.organisasjonsnummer }

                ArrangorflatePrincipal(organisasjonsnummer, JWTPrincipal(credentials.payload))
            }
        }
    }
}

data class ArrangorflatePrincipal(val organisasjonsnummer: List<Organisasjonsnummer>, val principal: JWTPrincipal)
