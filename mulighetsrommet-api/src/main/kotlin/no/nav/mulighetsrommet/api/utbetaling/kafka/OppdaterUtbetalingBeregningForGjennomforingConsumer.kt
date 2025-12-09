package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
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
        when (val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingV2Dto>(message)) {
            is TiltaksgjennomforingV2Dto.Enkeltplass -> Unit

            is TiltaksgjennomforingV2Dto.Gruppe -> db.session {
                if (harGjennomforingGenererteUtbetalinger(gjennomforing.id)) {
                    skedulerOppdateringAvUtbetaling(gjennomforing.id)
                }
            }
        }
    }

    private fun QueryContext.harGjennomforingGenererteUtbetalinger(gjennomforingId: UUID): Boolean {
        return queries.utbetaling.getByGjennomforing(gjennomforingId).any {
            when (it.status) {
                UtbetalingStatusType.INNSENDT,
                UtbetalingStatusType.TIL_ATTESTERING,
                UtbetalingStatusType.RETURNERT,
                UtbetalingStatusType.FERDIG_BEHANDLET,
                UtbetalingStatusType.DELVIS_UTBETALT,
                UtbetalingStatusType.UTBETALT,
                UtbetalingStatusType.AVBRUTT,
                -> false

                UtbetalingStatusType.GENERERT -> true
            }
        }
    }

    private fun QueryContext.skedulerOppdateringAvUtbetaling(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        oppdaterUtbetaling.schedule(gjennomforingId, offsetITilfelleDetErMangeEndringerForGjennomforing, session)
    }
}
