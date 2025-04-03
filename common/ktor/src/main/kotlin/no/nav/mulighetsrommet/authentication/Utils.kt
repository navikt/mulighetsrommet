package no.nav.mulighetsrommet.authentication

import io.ktor.server.auth.*
import io.ktor.server.routing.*

/**
 * Utility that requires all [AuthProvider]'s specified in [configurations] to authenticate the request.
 */
fun Route.authenticate(
    vararg configurations: String,
    build: Route.() -> Unit,
): Route {
    return authenticate(
        configurations = configurations.map { it }.toTypedArray(),
        strategy = AuthenticationStrategy.Required,
        build = build,
    )
}
