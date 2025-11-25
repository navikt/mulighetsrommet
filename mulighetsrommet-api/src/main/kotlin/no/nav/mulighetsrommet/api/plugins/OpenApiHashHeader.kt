package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

class OpenApiHashHeaderConfig {
    lateinit var hashAttributeKey: AttributeKey<String>
}

val OpenApiHashHeader =
    createRouteScopedPlugin(
        name = "OpenApiHash",
        createConfiguration = ::OpenApiHashHeaderConfig,
    ) {
        val log = LoggerFactory.getLogger("OpenApiHashHeader")

        onCall { call ->
            call.application.attributes.getOrNull(pluginConfig.hashAttributeKey)?.let {
                call.response.headers.append("X-OpenAPI-Hash", it)
            } ?: log.error("\"${pluginConfig.hashAttributeKey}\" var ikke satt. Ikke ok i prod")
        }
    }
