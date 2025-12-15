package no.nav.mulighetsrommet.arena.adapter.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.mulighetsrommet.arena.adapter.AuthConfig
import java.net.URI
import java.util.concurrent.TimeUnit

fun Application.configureAuthentication(
    auth: AuthConfig,
) {
    val (azure) = auth

    val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt {
            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
