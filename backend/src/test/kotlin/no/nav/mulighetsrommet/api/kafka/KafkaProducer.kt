package no.nav.mulighetsrommet.api.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

// Denne er kun ment som en måte å produsere tilsvarende events inn på køene for å utvikle/test
fun main(args: Array<String>): Unit = runBlocking {
    val properties = KafkaPropertiesBuilder.producerBuilder()
        .withBrokerUrl("localhost:29092")
        .withBaseProperties()
        .withProducerId("mulighetsrommet-api-producer.v1")
        .withSerializers(StringSerializer::class.java, StringSerializer::class.java)
        .build()

    val producer = KafkaProducerClientBuilder.builder<String, String>()
        .withProperties(properties)
        .build()

    launch {
        produceTiltakEndretEvents(producer)
        producer.close()
    }
}

private suspend fun produceTiltakEndretEvents(producer: KafkaProducerClient<String, String>) {
    tiltakEndretTopic.forEach { it ->
        producer.send(ProducerRecord("teamarenanais.aapen-arena-tiltakendret-v1-q2", it.first, it.second))
        delay(5000)
    }
}
