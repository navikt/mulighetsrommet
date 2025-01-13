package no.nav.mulighetsrommet.api.arrangor.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotliquery.Session
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class AmtVirksomheterV1KafkaConsumer(
    config: Config,
    private val db: Database,
    private val brregClient: BrregClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement): Unit = db.session {
        if (shouldIgnoreMessage(key)) {
            return@session
        }

        val amtVirksomhet = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtVirksomhetV1Dto?>(message)
        if (amtVirksomhet != null) {
            updateVirksomhet(amtVirksomhet)
        } else {
            logger.info("Mottok tombstone for virksomhet med orgnr=$key, sletter virksomheten")
            Queries.arrangor.delete(key)
        }
    }

    context(Session)
    private suspend fun updateVirksomhet(amtVirksomhet: AmtVirksomhetV1Dto) {
        brregClient.getBrregVirksomhet(amtVirksomhet.organisasjonsnummer)
            .onRight { virksomhet ->
                Queries.arrangor.upsert(virksomhet)
            }
            .onLeft { error ->
                logger.error("Error when syncing orgnr: ${amtVirksomhet.organisasjonsnummer} from brreg in AmtVirksomhetV1TopicConsumer")
                throw IllegalStateException("Forventet å finne virksomhet med orgnr ${amtVirksomhet.organisasjonsnummer} i Brreg. Er det feil data i meldingen? Respons fra Brreg: $error")
            }
    }

    context(Session)
    private fun shouldIgnoreMessage(key: String): Boolean {
        return Queries.arrangor.get(Organisasjonsnummer(key)) == null
    }
}
