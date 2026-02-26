package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtArrangorMelding
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.UUID

class AmtArrangorMeldingV1KafkaConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer arrangor-melding med id=$key")

        val gjennomforingId = when (val melding = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtArrangorMelding?>(message)) {
            is AmtArrangorMelding.Forslag -> {
                when (melding.status) {
                    is AmtArrangorMelding.Forslag.Status.Avvist,
                    is AmtArrangorMelding.Forslag.Status.Erstattet,
                    is AmtArrangorMelding.Forslag.Status.Godkjent,
                    is AmtArrangorMelding.Forslag.Status.Tilbakekalt,
                    -> {
                        val gjennomforingId = queries.deltakerForslag.get(key)?.let {
                            queries.deltaker.get(it.deltakerId)?.gjennomforingId
                        }
                        queries.deltakerForslag.delete(melding.id)
                        gjennomforingId
                    }

                    AmtArrangorMelding.Forslag.Status.VenterPaSvar -> {
                        val deltaker = queries.deltaker.get(melding.deltakerId)
                        if (deltaker != null) {
                            queries.deltakerForslag.upsert(melding.toForslagDbo())
                        }
                        deltaker?.gjennomforingId
                    }
                }
            }

            null -> {
                val gjennomforingId = queries.deltakerForslag.get(key)?.let {
                    queries.deltaker.get(it.deltakerId)?.gjennomforingId
                }
                queries.deltakerForslag.delete(key)
                gjennomforingId
            }
        }

        gjennomforingId?.let {
            genererUtbetalingService.oppdaterUtbetalingBlokkeringerForGjennomforing(it)
        }
    }
}

fun AmtArrangorMelding.Forslag.toForslagDbo(): DeltakerForslag {
    return DeltakerForslag(
        id = id,
        deltakerId = deltakerId,
        endring = endring,
        status = status.toStatus(),
    )
}

fun AmtArrangorMelding.Forslag.Status.toStatus(): DeltakerForslag.Status = when (this) {
    is AmtArrangorMelding.Forslag.Status.Avvist -> DeltakerForslag.Status.AVVIST
    is AmtArrangorMelding.Forslag.Status.Erstattet -> DeltakerForslag.Status.ERSTATTET
    is AmtArrangorMelding.Forslag.Status.Godkjent -> DeltakerForslag.Status.GODKJENT
    is AmtArrangorMelding.Forslag.Status.Tilbakekalt -> DeltakerForslag.Status.TILBAKEKALT
    AmtArrangorMelding.Forslag.Status.VenterPaSvar -> DeltakerForslag.Status.VENTER_PA_SVAR
}
