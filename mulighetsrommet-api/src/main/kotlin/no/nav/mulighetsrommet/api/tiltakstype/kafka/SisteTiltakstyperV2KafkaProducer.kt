package no.nav.mulighetsrommet.api.tiltakstype.kafka

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.model.TiltakstypeEksternV2Dto
import org.apache.kafka.clients.producer.ProducerRecord

class SisteTiltakstyperV2KafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publish(value: TiltakstypeEksternV2Dto) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            value.id.toString(),
            Json.encodeToString(value),
        )
        kafkaProducerClient.sendSync(record)
    }
}
