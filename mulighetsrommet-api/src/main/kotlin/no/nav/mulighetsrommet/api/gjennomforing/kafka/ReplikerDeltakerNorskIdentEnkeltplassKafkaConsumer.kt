package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.UUID

class ReplikerDeltakerNorskIdentEnkeltplassKafkaConsumer(
    private val db: ApiDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        // TODO: Håndter tombstones og feilregistrerte deltakere:
        //  - Skal dette føre til at gjennomføring også slettes?
        //  - Hvis ikke, støtte oppdatering av fritekstsøk (fjerne fnr fra fts)
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)
            ?: error("Mottok tombstone for deltaker med id=$key, dette støttes ikke ennå")

        val gjennomforing = queries.gjennomforing.getGjennomforing(amtDeltaker.gjennomforingId)
        if (gjennomforing !is GjennomforingEnkeltplass) {
            return
        }

        val deltaker = queries.deltaker.get(key) ?: error("Deltaker med id=$key har ikke blitt replikert ennå")
        if (erMeldingUtdatert(deltaker, amtDeltaker)) {
            return
        }

        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, listOf(amtDeltaker.personIdent))
    }

    private fun erMeldingUtdatert(deltaker: Deltaker, amtDeltaker: AmtDeltakerEksternV1Dto): Boolean {
        return deltaker.endretTidspunkt > amtDeltaker.endretTidspunkt
    }
}
