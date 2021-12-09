package no.nav.amt_informasjon_api.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.selectAll
import java.time.Duration

class KafkaFactory {

    private val streamsConfiguration = KafkaStreamConfig()
    private val kafkaStreams: KafkaStreams
    private val topology: Topology
//    private val adminClient: AdminClient

    init {
        topology = buildStream()
        kafkaStreams = KafkaStreams(topology, streamsConfiguration)
        kafkaStreams.cleanUp()
        kafkaStreams.start()
        println("KAFKA STATE: ${kafkaStreams.state().name}")
    }

    private fun buildStream(): Topology {
        val builder = StreamsBuilder()
        builder.stream<String, String>(KafkaTopics.Tiltaksgjennomforing.topic)
        return builder.build()
    }

//    fun shutdown() {
//        kafkaStreams.close()
//    }
//
//    fun isAlive(): Boolean {
//        return kafkaStreams.state().isRunningOrRebalancing
//    }

    // private fun createConsumer(): Consumer<String, String> {
    //     val props = streamsConfiguration
    //     props["key.deserializer"] = StringDeserializer::class.java
    //     props["value.deserializer"] = StringDeserializer::class.java
    //     return KafkaConsumer(props)
    // }
    //
    // fun consumeArenaEvents() {
    //     val consumer = createConsumer()
    //     consumer.subscribe(listOf(KafkaTopics.Tiltaksgjennomforing.topic))
    //     while (true) {
    //         val records = consumer.poll(Duration.ofSeconds(1))
    //         if (!records.isEmpty) {
    //             println("Consumed ${records.count()} records")
    //             records.iterator().forEach {
    //                 val message = it.value()
    //                 println("Message: $message")
    //             }
    //         }
    //     }
    // }
}

/**
 * Only for testing
 */
fun <T : IntIdTable> getRandomId(table: T): Int {
    return table
        .slice(table.id)
        .selectAll()
        .map { it[table.id].value }
        .random()
}
