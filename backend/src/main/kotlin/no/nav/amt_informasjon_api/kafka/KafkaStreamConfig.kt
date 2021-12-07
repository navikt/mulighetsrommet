package no.nav.amt_informasjon_api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import java.util.*

class KafkaStreamConfig : Properties() {
    private val appConfig = HoconApplicationConfig(ConfigFactory.load())

    init {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = appConfig.property("ktor.kafka.appId").getString()
        this[StreamsConfig.CLIENT_ID_CONFIG] = appConfig.property("ktor.kafka.clientId").getString()
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] =
            "${appConfig.property("ktor.kafka.host").getString()}:${appConfig.property("ktor.kafka.port").getString()}"
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
        this[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 10 * 1000
        this[StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG] = 0
    }
}