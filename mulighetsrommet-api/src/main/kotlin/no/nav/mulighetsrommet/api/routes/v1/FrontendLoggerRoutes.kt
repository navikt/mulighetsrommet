package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.metrics.Event
import no.nav.common.metrics.MetricsClient
import no.nav.common.utils.EnvironmentUtils
import org.koin.ktor.ext.inject

fun Route.frontendLoggerRoutes() {
    val metricsClient: MetricsClient by inject()
    val logger = application.environment.log

    route("/api/v1/logger/") {
        post("event") {
            runCatching {
                val event = call.receive<FrontendEvent>()
                val toInflux = Event(event.name + ".event")
                event.tags.forEach(toInflux::addTagToReport)
                event.fields.forEach(toInflux::addFieldToReport)
                EnvironmentUtils.setProperty(
                    EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME,
                    "mulighetsrommet-api",
                    EnvironmentUtils.Type.PUBLIC
                )
                EnvironmentUtils.setProperty(
                    EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME,
                    "dev-gcp",
                    EnvironmentUtils.Type.PUBLIC
                )
                EnvironmentUtils.setProperty(
                    EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME,
                    "team-mulighetsrommet",
                    EnvironmentUtils.Type.PUBLIC
                )
                // toInflux.tags["environment"] = if (EnvironmentUtils.isProduction().orElse(false)) "p" else "q1"
                println()
                println("Event: ")
                println(toInflux.eventToString(event.name))
                // println(EnvironmentUtils.isProduction())
                println()
                metricsClient.report(toInflux)
                call.respond(toInflux.eventToString(event.name))
            }.onSuccess {
            }.onFailure {
                logger.error("${this.context.request.path()} ${it.stackTraceToString()}")
                call.respondText("Kunne ikke oppdatere tiltakstype", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}

fun Event.eventToString(name: String): String {
    return (
        "name: " + name + ".event, fields: " + (fields?.entries ?: "[]") +
            ", tags: " + (tags?.entries ?: "[]")
        )
}

@Serializable
data class FrontendEvent(
    var name: String,
    var fields: Map<String, String>,
    var tags: Map<String, String>
)
