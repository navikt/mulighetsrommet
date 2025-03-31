package no.nav.mulighetsrommet.api.tiltakstype.kafka

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.model.TiltakstypeEksternV2Dto
import org.apache.kafka.clients.producer.ProducerRecord

class SisteTiltakstyperV2KafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publish(value: TiltakstypeEksternV2Dto) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            value.id.toString().toByteArray(),
            Json.encodeToString(value).toByteArray(),
        )
        kafkaProducerClient.sendSync(record)
    }
}
