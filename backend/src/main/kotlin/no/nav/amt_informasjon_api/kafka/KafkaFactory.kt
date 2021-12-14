package no.nav.amt_informasjon_api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.selectAll
import java.util.Properties
import java.util.function.Consumer

class KafkaFactory {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    // private val streamsConfiguration = KafkaStreamConfig()
    // private val kafkaStreams: KafkaStreams
    // private val topology: Topology
    private val client: KafkaConsumerClient
//    private val adminClient: AdminClient
    private val properties: Properties

    init {
        properties = if (appConfig.property("ktor.development").getString() == "true") {
            KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withBaseProperties()
                .withConsumerGroupId("amt-informasjon-api-consumer.v2")
                .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                .build()
        } else {
            KafkaPropertiesPreset.aivenDefaultConsumerProperties("amt-informasjon-api-consumer.v2")
        }

        val arenaTiltakTopic = "teamarenanais.aapen-arena-tiltakendret-v1-q2"
        val topicConfig = KafkaConsumerClientBuilder.TopicConfig<String, String>()
            .withLogging()
            .withConsumerConfig(
                arenaTiltakTopic,
                stringDeserializer(),
                stringDeserializer(),
                Consumer<ConsumerRecord<String, String>> { printTopicContent(it.value()) }
            )
        client = KafkaConsumerClientBuilder.builder()
            .withProperties(properties)
            .withTopicConfig(topicConfig)
            .build()

        client.start()
        // topology = buildStream()
        // kafkaStreams = KafkaStreams(topology, streamsConfiguration)
        // kafkaStreams.cleanUp()
        // kafkaStreams.start()
        // println("KAFKA STATE: ${kafkaStreams.state().name}")
    }

    // private fun buildStream(): Topology {
    //     val builder = StreamsBuilder()
    //     builder.stream<String, String>(KafkaTopics.Tiltaksgjennomforing.topic)
    //     return builder.build()
    // }

    private fun printTopicContent(value: String) {
        println("TOPIC: $value")
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
