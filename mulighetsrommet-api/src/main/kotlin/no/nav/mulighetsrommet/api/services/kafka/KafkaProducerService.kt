package no.nav.mulighetsrommet.api.services.kafka

import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaProducerService<K, V>(private val kafkaProducerClient: KafkaProducerClient<K, V>) {
    fun produserMelding(topic: String, key: K, value: V) {
        kafkaProducerClient.sendSync(ProducerRecord(topic, key, value))
    }
}
