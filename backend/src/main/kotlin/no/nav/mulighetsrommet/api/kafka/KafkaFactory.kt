package no.nav.mulighetsrommet.api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.database.DatabaseFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.selectAll
import java.util.function.Consumer

class KafkaFactory(private val db: DatabaseFactory) {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val consumerClient: KafkaConsumerClient

    init {
        val consumerProperties = if (appConfig.property("ktor.localDevelopment").getString() == "true") {
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
                Consumer<ConsumerRecord<String, String>> { printTopicContent(it) }
            )

        // val testTopicConfig = KafkaConsumerClientBuilder.TopicConfig<String, String>()
        //     .withLogging()
        //     .withConsumerConfig(
        //         "aura.kafkarator-canary-dev-gcp",
        //         stringDeserializer(),
        //         stringDeserializer(),
        //         Consumer<ConsumerRecord<String, String>> { printTopicContent(it) }
        //     )

        val topics = listOf(topicConfig)

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withTopicConfigs(topics)
            .build()

        consumerClient.start()
    }

    fun stopClient() {
        consumerClient.stop()
    }

    private fun printTopicContent(consumerRecord: ConsumerRecord<String, String>) {
        println("TOPIC (${consumerRecord.topic()}): ${consumerRecord.value()}")
    }
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
