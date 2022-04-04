package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.UrlJwkProvider
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.request.*
import no.nav.mulighetsrommet.api.AuthProvider
import java.net.URI

fun Application.configureAuthentication(auth: Map<String, AuthProvider>) {

    install(Authentication) {
        jwt("mulighetsrommet-auth") {
            val config = auth.getOrElse("azure") { throw RuntimeException("Azure auth provider is missing") }

            val jwkProvider = UrlJwkProvider(URI(config.jwksUri).toURL())

            // TODO: Include realm?
            // realm = config.application.name

            verifier(jwkProvider, config.issuer) {
                withAudience(config.audience)
            }

            validate { credentials ->
                // TODO: validate user from nav-ident header
                // val navIdent = request.header("nav-ident")

                JWTPrincipal(credentials.payload)
            }
        }
    }
}
