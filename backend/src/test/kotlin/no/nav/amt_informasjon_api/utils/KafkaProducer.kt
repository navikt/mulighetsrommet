package no.nav.amt_informasjon_api.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.amt_informasjon_api.kafka.KafkaTopics
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import java.util.*

@ExperimentalSerializationApi
fun main() {

    // Må finne en bedre løsning på å hardkode settings sånn. Usikker om vi skal ha denne i fremtiden.
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] = "arbeidstiltak-api"
    props[StreamsConfig.CLIENT_ID_CONFIG] = "arbeidstiltak-api"
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:10002"
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName

    val numMessages = 10
    // `use` will execute block and close producer automatically
    KafkaProducer<String, String>(props).use { producer ->
        repeat(numMessages) { i ->
            val key = "created_tiltak"
            val record = Json.encodeToString(ArenaEvent(i))
            println("Producing record: $key\t${record}")

            producer.send(
                ProducerRecord(
                    KafkaTopics.Tiltaksgjennomforing.topic,
                    key,
                    record
                )
            ) { m: RecordMetadata, e: Exception? ->
                when (e) {
                    // no exception, good to go!
                    null -> println("Produced record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()}")
                    // print stacktrace in case of exception
                    else -> e.printStackTrace()
                }
            }
        }
        producer.flush()
        println("10 messages were produced to topic ${KafkaTopics.Tiltaksgjennomforing.topic}")
    }

}