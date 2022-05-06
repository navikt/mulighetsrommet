package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureMonitoring
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.internalRoutes
import org.slf4j.LoggerFactory

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")
    val preset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.app.kafka.consumerGroupId)

    val mulighetsrommetApiClient = MulighetsrommetApiClient(config.app.endpoints.get("mulighetsrommetApi")!!)

    initializeServer(config, Kafka(config.app.kafka, preset, Database(config.app.database), mulighetsrommetApiClient))
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
        internalRoutes()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.stopClient()
    }
}
