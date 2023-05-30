package no.nav.mulighetsrommet.kafka.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class AmtVirksomheterV1TopicConsumer(
    config: Config,
    private val virksomhetRepository: VirksomhetRepository,
    private val brregClient: BrregClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        when (val amtVirksomhet = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtVirksomhetV1Dto?>(message)) {
            null -> {
                logger.info("Mottok tombstone for virksomhet med orgnr=$key, sletter virksomheten")
                virksomhetRepository.delete(key)
            }
            else -> {
                val enhet = brregClient.hentEnhet(amtVirksomhet.organisasjonsnummer) // Hent fra Brreg for å oppdatere postnummer og poststed
                virksomhetRepository.upsert(enhet)
            }
        }
    }

    private fun AmtVirksomhetV1Dto.toVirksomhetDto() = VirksomhetDto(
        navn = navn,
        organisasjonsnummer = organisasjonsnummer,
        overordnetEnhet = overordnetEnhetOrganisasjonsnummer,
    )
}
