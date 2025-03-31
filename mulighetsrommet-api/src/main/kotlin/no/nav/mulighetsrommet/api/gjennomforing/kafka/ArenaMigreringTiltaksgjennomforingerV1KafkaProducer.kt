package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaMigreringTiltaksgjennomforingDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class ArenaMigreringTiltaksgjennomforingerV1KafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val config: Config,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val topic: String,
    )

    fun publish(value: ArenaMigreringTiltaksgjennomforingDto) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            value.id.toString().encodeToByteArray(),
            Json.encodeToString(value).toByteArray(),
        )

        logger.info("publish på ${config.topic} id: ${value.id}")
        kafkaProducerClient.sendSync(record)
    }
}
