package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.replikering.VirksomhetReplikering

class ReplikerAmtVirksomheterV1KafkaConsumer(
    private val replikering: VirksomhetReplikering,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        val organisasjonsnummer = Organisasjonsnummer(key)

        if (message == JsonNull) {
            replikering.deleteVirksomhet(organisasjonsnummer)
            return
        }

        replikering.syncVirksomhetIfExists(organisasjonsnummer)
    }
}
