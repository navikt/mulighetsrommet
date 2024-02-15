package no.nav.mulighetsrommet.kafka.producers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.env.NaisEnv
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
        val tiltakstyper: List<String> = emptyList(),
    )

    fun publish(value: ArenaMigreringTiltaksgjennomforingDto) {
        if (!config.tiltakstyper.contains(value.tiltakskode)) {
            return
        }
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            value.id.toString(),
            Json.encodeToString(value),
        )

        if (NaisEnv.current().isDevGCP()) {
            logger.info("publish på ${config.topic} id: ${value.id}")
            kafkaProducerClient.sendSync(record)
        }
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            id.toString(),
            null,
        )

        if (NaisEnv.current().isDevGCP()) {
            logger.info("retract på ${config.topic} id: $id")
            kafkaProducerClient.sendSync(record)
        }
    }
}
