package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import org.slf4j.LoggerFactory
import java.util.*

class AmtDeltakerV1KafkaConsumer(
    config: Config,
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            amtDeltaker == null -> {
                logger.info("Mottok tombstone for deltaker med id=$key, sletter deltakeren")
                queries.deltaker.deleteKometDeltaker(key)
            }

            amtDeltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker med id=$key fordi den var feilregistrert")
                queries.deltaker.deleteKometDeltaker(key)
            }

            else -> {
                logger.info("Forsøker å lagre deltaker med id=$key")
                query { queries.deltaker.upsertKometDeltaker(amtDeltaker) }.onLeft {
                    logger.warn("Feil under konsumering av deltaker med id=$key", it.error)
                    throw it.error
                }
            }
        }
    }
}
