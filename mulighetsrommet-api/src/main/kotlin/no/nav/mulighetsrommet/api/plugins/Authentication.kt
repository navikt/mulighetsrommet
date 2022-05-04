package no.nav.mulighetsrommet.api.plugins

import com.auth0.jwk.UrlJwkProvider
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import no.nav.mulighetsrommet.api.AuthConfig
import java.net.URI

fun Application.configureAuthentication(auth: AuthConfig) {
    val (azure) = auth

    install(Authentication) {
        jwt {
            val jwkProvider = UrlJwkProvider(URI(azure.jwksUri).toURL())

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
