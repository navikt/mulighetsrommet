package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.kafka.plugins.configureHTTP
import no.nav.mulighetsrommet.kafka.plugins.configureMonitoring
import no.nav.mulighetsrommet.kafka.plugins.configureSerialization
import no.nav.mulighetsrommet.kafka.routes.healthRoutes
import org.slf4j.LoggerFactory

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")
    val preset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.app.kafka.consumerGroupId)

    val client = HttpClient {
        defaultRequest {
            url.takeFrom(
                URLBuilder().takeFrom(config.app.endpoints.get("mulighetsrommetApi")!!).apply {
                    encodedPath += url.encodedPath
                }
            )
        }
    }
    initializeServer(config, Kafka(config.app.kafka, preset, Database(config.app.database), client))
}

fun initializeServer(config: Config, kafka: Kafka) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                main(kafka)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
}

fun Application.main(kafka: Kafka) {

    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        healthRoutes()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.stopClient()
    }
}
