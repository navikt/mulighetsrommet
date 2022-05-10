package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import no.nav.mulighetsrommet.api.AuthConfig
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.net.URI
import java.util.concurrent.TimeUnit

fun Application.configureAuthentication(auth: AuthConfig) {
    val (azure) = auth

    install(Authentication) {
        jwt {
            val jwkProvider = JwkProviderBuilder(URI(azure.jwksUri).toURL())
                .cached(5, 12, TimeUnit.HOURS)
                .build()

            verifier(jwkProvider, azure.issuer) {
                withAudience(azure.audience)
            }

            validate { credentials ->
                // TODO: validate user from nav-ident header
                // val navIdent = request.header("nav-ident")

                JWTPrincipal(credentials.payload)
            }
        }
    }
}
