package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.time.Instant
import java.util.*

class OppdaterUtbetalingBeregningForGjennomforingConsumer(
    private val db: ApiDatabase,
    private val oppdaterUtbetaling: OppdaterUtbetalingBeregning,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingV1Dto?>(message)
            ?: throw UnsupportedOperationException("Sletting av utbetalinger er ikke støttet. Tombstones er derfor ikke tillatt.")

        scheduleOppdateringAvUtbetaling(gjennomforing.id)
    }

    private fun scheduleOppdateringAvUtbetaling(gjennomforingId: UUID) = db.session {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        oppdaterUtbetaling.schedule(gjennomforingId, offsetITilfelleDetErMangeEndringerForGjennomforing, session)
    }
}
