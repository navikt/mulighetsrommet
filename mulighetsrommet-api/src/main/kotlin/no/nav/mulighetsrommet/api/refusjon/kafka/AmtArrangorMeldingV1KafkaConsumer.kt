package no.nav.mulighetsrommet.api.refusjon.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslag
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtArrangorMeldingV1KafkaConsumer(
    config: Config,
    private val db: Database,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.tx {
        logger.info("Konsumerer arrangor-melding med id=$key")

        when (val melding = JsonIgnoreUnknownKeys.decodeFromJsonElement<Melding?>(message)) {
            is Melding.Forslag -> {
                when (melding.status) {
                    is Melding.Forslag.Status.Avvist,
                    is Melding.Forslag.Status.Erstattet,
                    is Melding.Forslag.Status.Godkjent,
                    is Melding.Forslag.Status.Tilbakekalt,
                    -> {
                        Queries.deltakerForslag.delete(melding.id)
                    }

                    Melding.Forslag.Status.VenterPaSvar -> {
                        if (Queries.deltaker.get(melding.deltakerId) != null) {
                            Queries.deltakerForslag.upsert(melding.toForslagDbo())
                        }
                    }
                }
            }

            null -> Queries.deltakerForslag.delete(key)
        }
    }
}

fun Melding.Forslag.toForslagDbo(): DeltakerForslag {
    return DeltakerForslag(
        id = this.id,
        deltakerId = this.deltakerId,
        endring = this.endring,
        status = this.status.toStatus(),
    )
}

fun Melding.Forslag.Status.toStatus(): DeltakerForslag.Status = when (this) {
    is Melding.Forslag.Status.Avvist -> DeltakerForslag.Status.AVVIST
    is Melding.Forslag.Status.Erstattet -> DeltakerForslag.Status.ERSTATTET
    is Melding.Forslag.Status.Godkjent -> DeltakerForslag.Status.GODKJENT
    is Melding.Forslag.Status.Tilbakekalt -> DeltakerForslag.Status.TILBAKEKALT
    Melding.Forslag.Status.VenterPaSvar -> DeltakerForslag.Status.VENTER_PA_SVAR
}
