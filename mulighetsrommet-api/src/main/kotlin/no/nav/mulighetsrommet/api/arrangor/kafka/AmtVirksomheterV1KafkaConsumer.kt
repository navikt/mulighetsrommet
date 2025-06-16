package no.nav.mulighetsrommet.api.arrangor.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class AmtVirksomheterV1KafkaConsumer(
    private val arrangorService: ArrangorService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        if (shouldIgnoreMessage(key)) {
            return
        }

        val amtVirksomhet = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtVirksomhetV1Dto?>(message)
        if (amtVirksomhet != null) {
            updateVirksomhet(amtVirksomhet)
        } else {
            logger.info("Mottok tombstone for virksomhet med orgnr=$key, sletter virksomheten")
            arrangorService.deleteArrangor(Organisasjonsnummer(key))
        }
    }

    private suspend fun updateVirksomhet(amtVirksomhet: AmtVirksomhetV1Dto) {
        arrangorService.syncArrangorFromBrreg(amtVirksomhet.organisasjonsnummer).onLeft { error ->
            when (error) {
                is BrregError.FjernetAvJuridiskeArsaker -> {
                    logger.warn("Virksomhet med orgnr=${amtVirksomhet.organisasjonsnummer} er fjernet fra Brreg")
                    return
                }

                else -> {
                    logger.error("Feil ved sync av orgnr ${amtVirksomhet.organisasjonsnummer} fra Brreg")
                    throw IllegalStateException("Forventet å finne virksomhet med orgnr ${amtVirksomhet.organisasjonsnummer} i Brreg. Er det feil data i meldingen? Error: $error")
                }
            }
        }
    }

    private fun shouldIgnoreMessage(key: String): Boolean {
        return arrangorService.getArrangor(Organisasjonsnummer(key)) == null
    }
}
