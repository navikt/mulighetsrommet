package no.nav.mulighetsrommet.api.datavarehus.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class DatavarehusTiltakV1KafkaProducer(
    private val config: Config,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val db: ApiDatabase,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {

    data class Config(
        val producerTopic: String,
    )

    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingV1Dto?>(message)

        if (gjennomforing != null) {
            publishDatavarehusTiltak(gjennomforing.id)
        } else {
            retractDatavarehusTiltak(UUID.fromString(key))
        }
    }

    private fun publishDatavarehusTiltak(id: UUID) = db.session {
        val dto = queries.dvh.getTiltak(id)

        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.producerTopic,
            dto.gjennomforing.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
        )

        kafkaProducerClient.sendSync(record)
    }

    private fun retractDatavarehusTiltak(id: UUID) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.producerTopic,
            id.toString().toByteArray(),
            null,
        )

        kafkaProducerClient.sendSync(record)
    }
}
