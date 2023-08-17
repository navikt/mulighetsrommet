package no.nav.mulighetsrommet.kafka.producers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class ArenaMigreringTiltaksgjennomforingKafkaProducer(
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publish(value: ArenaMigreringTiltaksgjennomforingDto) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            value.ID.toString(),
            Json.encodeToString(value),
        )
        // TODO: Disabled
        // kafkaProducerClient.sendSync(record)
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            id.toString(),
            null,
        )
        // TODO: Disabled
        // kafkaProducerClient.sendSync(record)
    }
}
