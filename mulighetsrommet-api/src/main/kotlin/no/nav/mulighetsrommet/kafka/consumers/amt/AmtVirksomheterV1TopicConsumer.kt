package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class AmtVirksomheterV1TopicConsumer(
    config: Config,
    private val virksomhetRepository: VirksomhetRepository,
    private val virksomhetService: VirksomhetService,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        if (shouldIgnoreMessage(key)) {
            return
        }

        when (val amtVirksomhet = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtVirksomhetV1Dto?>(message)) {
            null -> {
                logger.info("Mottok tombstone for virksomhet med orgnr=$key, sletter virksomheten")
                virksomhetRepository.delete(key)
            }

            else -> {
                virksomhetService.getVirksomhet(amtVirksomhet.organisasjonsnummer)
                    .onRight { virksomhet ->
                        // Hent fra Brreg for å oppdatere postnummer og poststed
                        virksomhetRepository.upsert(virksomhet)
                    }
                    .onLeft { error ->
                        throw IllegalStateException("Forventet å finne virksomhet med orgnr ${amtVirksomhet.organisasjonsnummer} i Brreg. Er det feil data i meldingen? Respons fra Brreg: $error")
                    }
            }
        }
    }

    private fun shouldIgnoreMessage(key: String): Boolean {
        return virksomhetRepository.get(key).getOrThrow() == null
    }
}
