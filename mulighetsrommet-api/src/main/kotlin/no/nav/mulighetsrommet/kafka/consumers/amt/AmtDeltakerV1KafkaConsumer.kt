package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtDeltakerV1KafkaConsumer(
    config: Config,
    private val deltakere: DeltakerRepository,
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
                deltakere.delete(key)
            }

            amtDeltaker.status.type == AmtDeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker med id=$key fordi den var feilregistrert")
                deltakere.delete(key)
            }

            else -> {
                logger.info("Lagrer deltaker med id=$key")
                val deltaker = amtDeltaker.toDeltakerDbo()
                deltakere.upsert(deltaker)
            }
        }
    }

    private fun AmtDeltakerV1Dto.toDeltakerDbo(): DeltakerDbo = DeltakerDbo(
        id = id,
        gjennomforingId = gjennomforingId,
        startDato = startDato,
        sluttDato = sluttDato,
        registrertTidspunkt = registrertDato,
        endretTidspunkt = endretDato,
        stillingsprosent = prosentStilling?.toDouble(),
        status = status,
    )
}
