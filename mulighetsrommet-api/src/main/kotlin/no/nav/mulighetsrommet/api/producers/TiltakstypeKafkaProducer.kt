package no.nav.mulighetsrommet.api.producers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class TiltakstypeKafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publish(value: TiltakstypeDto) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            value.id.toString(),
            Json.encodeToString(value),
        )
        kafkaProducerClient.sendSync(record)
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            id.toString(),
            null,
        )
        kafkaProducerClient.sendSync(record)
    }
}
