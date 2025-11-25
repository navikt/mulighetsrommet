package no.nav.tiltak.historikk.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import no.nav.tiltak.historikk.AuthConfig
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class AuthProvider {
    abstract val requiredRoles: List<String>
    val name: String
        get() = requiredRoles.joinToString(separator = "_", prefix = "[", postfix = "]")
}

object TiltakshistorikkRead : AuthProvider() {
    override val requiredRoles: List<String> = listOf(ACCESS_AS_APPLICATION, "tiltakshistorikk:read")
}

object TiltakshistorikkWrite : AuthProvider() {
    override val requiredRoles: List<String> = listOf(ACCESS_AS_APPLICATION, "tiltakshistorikk:write")
}

object TiltakshistorikkAdmin : AuthProvider() {
    override val requiredRoles: List<String> = listOf(ACCESS_AS_APPLICATION, "admin")
}

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

const val ACCESS_AS_APPLICATION = "access_as_application"

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    fun AuthenticationConfig.jwt(
        authProvider: AuthProvider,
    ) = jwt(authProvider.name) {
        verifier(jwkProvider, auth.azure.issuer) {
            withAudience(auth.azure.audience)
        }

        validate { credentials ->
            val roles = credentials.getListClaim("roles", String::class)

            if (authProvider.requiredRoles.any { it !in roles }) {
                return@validate null
            }

            JWTPrincipal(credentials.payload)
        }
    }

    install(Authentication) {
        jwt(TiltakshistorikkRead)
        jwt(TiltakshistorikkWrite)
        jwt(TiltakshistorikkAdmin)
    }
}
