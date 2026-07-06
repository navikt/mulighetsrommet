package no.nav.tiltak.historikk.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.Route
import no.nav.tiltak.historikk.AuthConfig
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class AuthProvider {
    TILTAKSHISTORIKK_READ,
    TILTAKSHISTORIKK_WRITE,
    TEAM_MULIGHETSROMMET,
}

val TiltakshistorikkReadRoles = listOf(ACCESS_AS_APPLICATION, "tiltakshistorikk-read")
val TiltakshistorikkWriteRoles = listOf(ACCESS_AS_APPLICATION, "tiltakshistorikk-write")

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

    fun AuthenticationConfig.jwtWithRoles(
        authProvider: AuthProvider,
        requiredRoles: List<String>,
    ) = jwt(authProvider.name) {
        verifier(jwkProvider, auth.azure.issuer) {
            withAudience(auth.azure.audience)
        }

        validate { credentials ->
            val roles = credentials.getListClaim("roles", String::class)

            if (requiredRoles.any { it !in roles }) {
                return@validate null
            }

            JWTPrincipal(credentials.payload)
        }
    }

    fun AuthenticationConfig.jwtWithGroups(
        authProvider: AuthProvider,
        requiredGroups: List<UUID>,
    ) = jwt(authProvider.name) {
        verifier(jwkProvider, auth.azure.issuer) {
            withAudience(auth.azure.audience)
        }

        validate { credentials ->
            val roles = credentials.getListClaim("groups", UUID::class)

            if (requiredGroups.any { it !in roles }) {
                return@validate null
            }

            JWTPrincipal(credentials.payload)
        }
    }

    install(Authentication) {
        jwtWithRoles(AuthProvider.TILTAKSHISTORIKK_READ, TiltakshistorikkReadRoles)
        jwtWithRoles(AuthProvider.TILTAKSHISTORIKK_WRITE, TiltakshistorikkWriteRoles)
        jwtWithGroups(AuthProvider.TEAM_MULIGHETSROMMET, listOf(auth.teamMulighetsrommetEntraAdGroupId))
    }
}
