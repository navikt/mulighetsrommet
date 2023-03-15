package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.server.webjars.*
import no.nav.mulighetsrommet.api.SwaggerConfig

fun Application.configureWebjars(swaggerConfig: SwaggerConfig?) {
    if (swaggerConfig?.enable == true) {
        install(Webjars) {
            path = "assets"
        }
    }
}
