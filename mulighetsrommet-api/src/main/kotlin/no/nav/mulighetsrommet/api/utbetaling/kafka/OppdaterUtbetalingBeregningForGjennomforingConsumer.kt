package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.time.Instant
import java.util.UUID

class OppdaterUtbetalingBeregningForGjennomforingConsumer(
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        when (val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingV2Dto>(message)) {
            is TiltaksgjennomforingV2Dto.Enkeltplass -> Unit

            is TiltaksgjennomforingV2Dto.Gruppe -> {
                skedulerOppdaterUtbetalinger(gjennomforing.id)
            }
        }
    }

    private fun skedulerOppdaterUtbetalinger(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(
            gjennomforingId = gjennomforingId,
            tidspunkt = offsetITilfelleDetErMangeEndringerForGjennomforing,
        )
    }
}
