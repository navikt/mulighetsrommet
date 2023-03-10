package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.SwaggerConfig

fun Application.configureSwagger(config: SwaggerConfig) {
    if (config.enable) {
        routing {
            swaggerUI(path = "/swagger-ui", swaggerFile = "web/openapi.yaml")
        }
    }
}
