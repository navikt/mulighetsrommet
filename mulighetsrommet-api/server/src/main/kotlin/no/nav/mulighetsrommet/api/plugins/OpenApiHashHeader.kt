package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.createRouteScopedPlugin
import no.nav.mulighetsrommet.api.routes.OpenApiSpec

class OpenApiVersionHeaderConfig {
    lateinit var spec: OpenApiSpec
}

val OpenApiVersionHeader =
    createRouteScopedPlugin(
        name = "OpenApiVersion",
        createConfiguration = ::OpenApiVersionHeaderConfig,
    ) {
        onCall { call ->
            call.response.headers.append("X-OpenAPI-Version", pluginConfig.spec.version.toString())
        }
    }
