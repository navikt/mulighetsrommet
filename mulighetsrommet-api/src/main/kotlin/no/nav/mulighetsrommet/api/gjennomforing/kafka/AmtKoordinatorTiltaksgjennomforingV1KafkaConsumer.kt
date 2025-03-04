package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.slf4j.LoggerFactory
import java.util.*

class AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer(
    config: Config,
    private val db: ApiDatabase,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer koordinator-tiltaksgjennomforing med id=$key")

        val melding = JsonIgnoreUnknownKeys.decodeFromJsonElement<Melding?>(message)

        when {
            melding == null -> {
                logger.info("Mottok tombstone for koordinator-tiltaksgjennomforing med id=$key, sletter koblingen mellom koordinator og gjennomføring")
                queries.gjennomforing.deleteKoordinatorForGjennomforing(navIdent = NavIdent(key))
            }

            else -> {
                logger.info("Upsert koordinator-tiltaksgjennomforing med id=$key")
                queries.gjennomforing.insertKoordinatorTilGjennomforing(
                    navIdent = melding.navIdent,
                    id = melding.tiltaksgjennomforingId,
                )
            }
        }
    }

    @Serializable
    data class Melding(
        val navIdent: NavIdent,
        @Serializable(with = UUIDSerializer::class)
        val tiltaksgjennomforingId: UUID,
    )
}
