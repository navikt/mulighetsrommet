package no.nav.mulighetsrommet.arena.adapter.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

// Denne er kun ment som en måte å produsere tilsvarende events inn på køene for å utvikle/test
fun main(): Unit = runBlocking {
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
//        produceTiltakEndretEvents(producer)
//        produceTiltakgjennomforingEndretEvents(producer)
//        produceTiltaksdeltakerEndretEvents(producer)
        produceSakEndretEvents(producer)
        producer.close()
    }
}

private suspend fun produceTiltakEndretEvents(producer: KafkaProducerClient<String, String>) {
    tiltakEndretTopic.forEach { it ->
        producer.send(ProducerRecord("tiltakendret", it.first, it.second))
        delay(500)
    }
}

private suspend fun produceTiltakgjennomforingEndretEvents(producer: KafkaProducerClient<String, String>) {
    tiltakgjennomforingEndretTopic.forEach { it ->
        producer.send(ProducerRecord("tiltakgjennomforingendret", it.first, it.second))
        delay(500)
    }
}

private suspend fun produceTiltaksdeltakerEndretEvents(producer: KafkaProducerClient<String, String>) {
    tiltakdeltakerTopic.forEach { it ->
        producer.send(ProducerRecord("tiltakdeltakerendret", it.first, it.second))
        delay(500)
    }
}

private suspend fun produceSakEndretEvents(producer: KafkaProducerClient<String, String>) {
    sakEndretTopic.forEach { it ->
        producer.send(ProducerRecord("sakendret", it.first, it.second))
        delay(500)
    }
}
