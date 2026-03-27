package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.utbetaling.kafka.toDeltaker
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.UUID

class ReplikerDeltakerEnkeltplassKafkaConsumer(
    private val db: ApiDatabase,
    private val service: GjennomforingEnkeltplassService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)
            ?: error("Mottok tombstone for deltaker med id=$key, dette skal ikke skje")

        val gjennomforing = queries.gjennomforing.getGjennomforing(amtDeltaker.gjennomforingId)
        if (gjennomforing !is GjennomforingEnkeltplass) {
            return
        }

        service.updateFromDeltaker(amtDeltaker.toDeltaker(), NorskIdent(amtDeltaker.personIdent))
    }
}
