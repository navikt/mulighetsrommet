package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.Melding
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtArrangorMeldingV1KafkaConsumer(
    private val db: ApiDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer arrangor-melding med id=$key")

        when (val melding = JsonIgnoreUnknownKeys.decodeFromJsonElement<Melding?>(message)) {
            is Melding.Forslag -> {
                when (melding.status) {
                    is Melding.Forslag.Status.Avvist,
                    is Melding.Forslag.Status.Erstattet,
                    is Melding.Forslag.Status.Godkjent,
                    is Melding.Forslag.Status.Tilbakekalt,
                    -> {
                        queries.deltakerForslag.delete(melding.id)
                    }

                    Melding.Forslag.Status.VenterPaSvar -> {
                        if (queries.deltaker.get(melding.deltakerId) != null) {
                            queries.deltakerForslag.upsert(melding.toForslagDbo())
                        }
                    }
                }
            }

            null -> queries.deltakerForslag.delete(key)
        }
    }
}

fun Melding.Forslag.toForslagDbo(): DeltakerForslag {
    return DeltakerForslag(
        id = id,
        deltakerId = deltakerId,
        endring = endring,
        status = status.toStatus(),
    )
}

fun Melding.Forslag.Status.toStatus(): DeltakerForslag.Status = when (this) {
    is Melding.Forslag.Status.Avvist -> DeltakerForslag.Status.AVVIST
    is Melding.Forslag.Status.Erstattet -> DeltakerForslag.Status.ERSTATTET
    is Melding.Forslag.Status.Godkjent -> DeltakerForslag.Status.GODKJENT
    is Melding.Forslag.Status.Tilbakekalt -> DeltakerForslag.Status.TILBAKEKALT
    Melding.Forslag.Status.VenterPaSvar -> DeltakerForslag.Status.VENTER_PA_SVAR
}
