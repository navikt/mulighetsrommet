package no.nav.mulighetsrommet.kafka.producers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

class ArenaMigreringTiltaksgjennomforingKafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val config: Config,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    data class Config(
        val topic: String,
    )

    fun publish(value: ArenaMigreringTiltaksgjennomforingDto) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            value.id.toString(),
            Json.encodeToString(value),
        )
        logger.info("(disabled) publish på ${config.topic} id: ${value.id}")
        // TODO: Disabled
        // kafkaProducerClient.sendSync(record)
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            id.toString(),
            null,
        )
        logger.info("(disabled) retract på ${config.topic} id: $id")
        // TODO: Disabled
        // kafkaProducerClient.sendSync(record)
    }
}
