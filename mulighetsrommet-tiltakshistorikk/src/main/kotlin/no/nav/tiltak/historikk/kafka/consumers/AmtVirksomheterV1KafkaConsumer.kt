package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.service.VirksomhetService
import org.slf4j.LoggerFactory

@Serializable
data class AmtVirksomhetV1Dto(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: Organisasjonsnummer?,
)

class AmtVirksomheterV1KafkaConsumer(
    private val virksomheter: VirksomhetService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        val organisasjonsnummer = Organisasjonsnummer(key)

        if (shouldIgnoreMessage(organisasjonsnummer)) {
            return
        }

        val melding = Json.decodeFromJsonElement<AmtVirksomhetV1Dto?>(message)
        if (melding == null) {
            logger.info("Mottok tombstone for virksomhet med orgnr=$key, sletter virksomheten")
            virksomheter.deleteVirksomhet(organisasjonsnummer)
            return
        }

        virksomheter.getAndSyncVirksomhet(organisasjonsnummer).onLeft { error ->
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
    }

    private fun shouldIgnoreMessage(organisasjonsnummer: Organisasjonsnummer): Boolean {
        return virksomheter.getVirksomhet(organisasjonsnummer) == null
    }
}
