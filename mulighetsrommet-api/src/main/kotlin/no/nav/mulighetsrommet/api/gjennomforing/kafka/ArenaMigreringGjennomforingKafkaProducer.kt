package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class ArenaMigreringGjennomforingKafkaProducer(
    private val config: Config,
    private val db: ApiDatabase,
    private val tiltakstyper: TiltakstypeService,
    private val arenaAdapterClient: ArenaAdapterClient,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) : KafkaTopicConsumer<String, JsonElement>(
    config.consumer,
    stringDeserializer(),
    JsonElementDeserializer(),
) {

    data class Config(
        val consumer: KafkaTopicConsumer.Config,
        val producerTopic: String,
    )

    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingEksternV1Dto?>(message)
            ?: throw UnsupportedOperationException("Arena støtter ikke sletting av gjennomføringer. Tombstone-meldinger er derfor ikke tillatt så lenge data må deles med Arena.")

        if (gjennomforingSkalDelesMedArena(gjennomforing)) {
            publishMigrertGjennomforing(gjennomforing.id)
        }
    }

    private suspend fun publishMigrertGjennomforing(id: UUID): Unit = db.session {
        val arenaGjennomforing = arenaAdapterClient.hentArenadata(id)

        val gjennomforing = checkNotNull(queries.gjennomforing.get(id))

        val migrertGjennomforing = ArenaMigreringTiltaksgjennomforingDto.from(
            gjennomforing,
            arenaGjennomforing?.arenaId,
        )

        publish(migrertGjennomforing)
    }

    private fun gjennomforingSkalDelesMedArena(gjennomforing: TiltaksgjennomforingEksternV1Dto): Boolean {
        return tiltakstyper.isEnabled(gjennomforing.tiltakstype.tiltakskode)
    }

    private fun publish(value: ArenaMigreringTiltaksgjennomforingDto) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.producerTopic,
            value.id.toString().encodeToByteArray(),
            Json.encodeToString(value).toByteArray(),
        )

        kafkaProducerClient.sendSync(record)
    }
}
