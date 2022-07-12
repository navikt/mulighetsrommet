package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.MDC

fun Route.frontendLoggerRoutes() {
    val logger = application.environment.log

    route("/api/v1/logger/") {
        post("event") {
            runCatching {
                val event = call.receive<FrontendEvent>()
                MDC.put("event.name", event.name)
                event.fields.forEach {
                    MDC.put("field.${it.key}", it.value)
                }
                event.tags.forEach {
                    MDC.put("tag.${it.key}", it.value)
                }
                logger.info("{}", event)
                event
            }.onSuccess { event ->
                call.respond(event)
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
            }
        }
    }
}

@Serializable
data class FrontendEvent(
    var name: String,
    var fields: Map<String, String>,
    var tags: Map<String, String>
)
