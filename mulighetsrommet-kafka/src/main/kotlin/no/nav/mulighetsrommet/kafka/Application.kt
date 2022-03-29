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

fun main(args: Array<String>) {
    val config = ConfigLoader().loadConfigOrThrow<AppConfig>("/application.yaml")
    val preset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.kafka.consumerGroupId)

    val client = HttpClient {
        defaultRequest {
            url.takeFrom(URLBuilder().takeFrom(config.endpoints.get("mulighetsrommetBackend")!!).apply {
                encodedPath += url.encodedPath
            })
        }
    }
    initializeServer(config, Kafka(config.kafka, preset, Database(config.database), client))
}

fun initializeServer(config: AppConfig, kafka: Kafka) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                main(config, kafka)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
}

fun Application.main(config: AppConfig, kafka: Kafka) {

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
