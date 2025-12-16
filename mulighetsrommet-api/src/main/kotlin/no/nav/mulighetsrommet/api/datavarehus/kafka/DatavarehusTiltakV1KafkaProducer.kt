package no.nav.mulighetsrommet.api.datavarehus.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

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
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingV2Dto?>(message)
            ?: throw UnsupportedOperationException("Støtter ikke tombstones av gjennomføringer")

        when (gjennomforing) {
            is TiltaksgjennomforingV2Dto.Gruppe -> publishGruppetiltak(gjennomforing.id)
            is TiltaksgjennomforingV2Dto.Enkeltplass -> publishEnkeltplass(gjennomforing.id)
        }
    }

    private fun publishGruppetiltak(id: UUID) = db.session {
        val tiltak = queries.dvh.getGruppetiltak(id)
        publish(tiltak)
    }

    private fun publishEnkeltplass(id: UUID) = db.session {
        val tiltak = queries.dvh.getEnkeltplass(id)
        publish(tiltak)
    }

    private fun publish(tiltak: DatavarehusTiltakV1) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.producerTopic,
            tiltak.gjennomforing.id.toString().toByteArray(),
            Json.encodeToString(tiltak).toByteArray(),
        )
        kafkaProducerClient.sendSync(record)
    }
}
