package no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import org.slf4j.LoggerFactory
import java.util.*

class AmtDeltakerV1Consumer(
    config: Config,
    private val deltakerRepository: DeltakerRepository,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            amtDeltaker == null -> {
                logger.info("Mottok tombstone for deltaker med id=$key, sletter deltakeren")
                deltakerRepository.deleteKometDeltaker(key)
            }

            amtDeltaker.status.type == AmtDeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker med id=$key fordi den var feilregistrert")
                deltakerRepository.deleteKometDeltaker(key)
            }

            else -> {
                logger.info("Forsøker å lagre deltaker med id=$key")
                query { deltakerRepository.upsertKometDeltaker(amtDeltaker) }
                    .onLeft {
                        logger.warn("Feil under konsumering av deltaker med id=$key", it.error)
                        throw it.error
                    }
            }
        }
    }
}
