package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class SisteTiltaksgjennomforingerV1KafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publish(value: TiltaksgjennomforingEksternV1Dto) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            value.id.toString().toByteArray(),
            Json.encodeToString(value).toByteArray(),
        )
        kafkaProducerClient.sendSync(record)
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            id.toString().toByteArray(),
            null,
        )
        kafkaProducerClient.sendSync(record)
    }
}
