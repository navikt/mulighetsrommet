package no.nav.mulighetsrommet.api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.database.DatabaseFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class EventProcessor(private val db: DatabaseFactory, private val topicMap: Map<String, String>) {

    private val logger = LoggerFactory.getLogger(EventProcessor::class.java)

    fun process(record: ConsumerRecord<String, String>) {
        when(record.topic()) {
            topicMap["tiltakendret"] -> insertTiltakstype(record.value())
        }
    }

    private fun insertTiltakstype(recordValue: String) {
        val json = Json.parseToJsonElement(recordValue).jsonObject
        println(recordValue)
    }
}
