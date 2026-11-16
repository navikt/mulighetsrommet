package no.nav.mulighetsrommet.maam

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.maam.routes.failedKafkaRecordRoutes
import no.nav.mulighetsrommet.maam.routes.kafkaTopicConsumerRoutes
import no.nav.mulighetsrommet.maam.routes.scheduledTaskRoutes
import no.nav.mulighetsrommet.maam.routes.taskRoutes

val Maam = createRouteScopedPlugin("Maam", ::MaamConfig) {
    val mount = requireNotNull(route) { "Maam must be installed on a route" }
    val database = pluginConfig.database
    val environment = pluginConfig.environment
    val kafka = pluginConfig.kafkaConfig
    val tasks = pluginConfig.tasksConfig

    if (kafka != null) {
        mount.route("/kafka") {
            if (kafka.consumersEnabled) {
                kafkaTopicConsumerRoutes(kafka.orchestrator)
            }

            if (kafka.failedRecordsEnabled) {
                val db = requireNotNull(database) {
                    "Maam requires a database provider for failed Kafka records"
                }
                failedKafkaRecordRoutes(db, environment)
            }
        }
    }

    if (tasks != null) {
        mount.route("/tasks") {
            if (tasks.scheduledEnabled) {
                val db = requireNotNull(database) {
                    "Maam requires a database provider for scheduled tasks"
                }
                scheduledTaskRoutes(db, environment)
            }
            if (tasks.handlers.isNotEmpty()) {
                taskRoutes(tasks)
            }
        }
    }
}
