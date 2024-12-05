package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.gjennomforing.db.DatavarehusGjennomforingQueries
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class DatavarehusGjennomforingV1KafkaProducer(
    consumerConfig: Config,
    private val producerTopic: String,
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val db: Database,
) : KafkaTopicConsumer<String, JsonElement>(
    consumerConfig,
    stringDeserializer(),
    JsonElementDeserializer(),
) {

    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingEksternV1Dto?>(message)

        if (gjennomforing != null) {
            publishDatavarehusGjennomforing(gjennomforing.id)
        } else {
            retractDatavarehusGjennomforing(UUID.fromString(key))
        }
    }

    private fun publishDatavarehusGjennomforing(id: UUID) = db.useSession {
        val dto = DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, id)

        val record: ProducerRecord<String, String?> = ProducerRecord(
            producerTopic,
            dto.id.toString(),
            Json.encodeToString(dto),
        )

        kafkaProducerClient.sendSync(record)
    }

    private fun retractDatavarehusGjennomforing(id: UUID) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            producerTopic,
            id.toString(),
            null,
        )

        kafkaProducerClient.sendSync(record)
    }
}
