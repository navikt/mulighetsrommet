package no.nav.mulighetsrommet.api.refusjon.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslag
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagRepository
import no.nav.mulighetsrommet.domain.dto.amt.EndringFraArrangor
import no.nav.mulighetsrommet.domain.dto.amt.Forslag
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtArrangorMeldingV1KafkaConsumer(
    config: Config,
    private val deltakerForslagRepository: DeltakerForslagRepository,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer arrangor-melding med id=$key")

        logger.debug("arrangor-melding: {}", message)
        val melding = JsonIgnoreUnknownKeys.decodeFromJsonElement<Melding?>(message)

        when (melding) {
            is EndringFraArrangor -> {
                // Aldri relevant
            }
            is Forslag -> {
                when (melding.status) {
                    is Forslag.Status.Avvist, is Forslag.Status.Erstattet,
                    is Forslag.Status.Godkjent, is Forslag.Status.Tilbakekalt,
                    -> deltakerForslagRepository.delete(melding.id)
                    Forslag.Status.VenterPaSvar -> deltakerForslagRepository.upsert(melding.toForslagDbo())
                }
            }
            null -> deltakerForslagRepository.delete(key)
        }
    }
}

fun Forslag.toForslagDbo(): DeltakerForslag {
    return DeltakerForslag(
        id = this.id,
        deltakerId = this.deltakerId,
        endring = this.endring,
        status = this.status.toStatus(),
    )
}

fun Forslag.Status.toStatus(): DeltakerForslag.Status = when (this) {
    is Forslag.Status.Avvist -> DeltakerForslag.Status.AVVIST
    is Forslag.Status.Erstattet -> DeltakerForslag.Status.ERSTATTET
    is Forslag.Status.Godkjent -> DeltakerForslag.Status.GODKJENT
    is Forslag.Status.Tilbakekalt -> DeltakerForslag.Status.TILBAKEKALT
    Forslag.Status.VenterPaSvar -> DeltakerForslag.Status.VENTERPASVAR
}
