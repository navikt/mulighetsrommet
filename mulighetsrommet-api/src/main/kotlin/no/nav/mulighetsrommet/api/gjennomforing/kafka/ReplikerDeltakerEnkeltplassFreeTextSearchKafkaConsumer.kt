package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ReplikerDeltakerEnkeltplassFreeTextSearchKafkaConsumer(
    private val config: RetryConfig = RetryConfig(),
    private val db: ApiDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    data class RetryConfig(
        val maxRetries: Int = 4,
        val delayIncrement: Duration = 2.seconds,
    )

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        // TODO: Håndter tombstones og feilregistrerte deltakere:
        //  - Skal dette føre til at gjennomføring også slettes?
        //  - Hvis ikke, støtte oppdatering av fritekstsøk (fjerne fnr fra fts)
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)
            ?: error("Mottok tombstone for deltaker med id=$key, dette støttes ikke enda")

        val gjennomforing = queries.gjennomforing.getGjennomforing(amtDeltaker.gjennomforingId)
        if (gjennomforing !is GjennomforingEnkeltplass) {
            return
        }

        val deltaker = getDeltaker(key)
        if (erMeldingUtdatert(deltaker, amtDeltaker)) {
            return
        }

        val personIdentHash = NorskIdentHasher.hashIfNorskIdent(amtDeltaker.personIdent)
        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, listOf(personIdentHash))
    }

    private suspend fun getDeltaker(id: UUID): Deltaker {
        repeat(config.maxRetries) { retries ->
            when (val deltaker = db.session { queries.deltaker.get(id) }) {
                null -> delay(config.delayIncrement * (retries + 1))
                else -> return deltaker
            }
        }

        return db.session { queries.deltaker.get(id) } ?: error("Deltaker med id=$id har ikke blitt replikert enda")
    }

    private fun erMeldingUtdatert(deltaker: Deltaker, amtDeltaker: AmtDeltakerEksternV1Dto): Boolean {
        return deltaker.endretTidspunkt > amtDeltaker.endretTidspunkt
    }
}
