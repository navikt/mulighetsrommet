package no.nav.mulighetsrommet.api.arrangor.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.admin.arrangor.DeleteArrangor
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangor
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorError
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class AmtVirksomheterV1KafkaConsumer(
    private val db: ApiDatabase,
    private val syncArrangor: SyncArrangorUseCase,
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
            syncArrangor.execute(DeleteArrangor(Organisasjonsnummer(key)))
        }
    }

    private suspend fun updateVirksomhet(amtVirksomhet: AmtVirksomhetV1Dto) {
        syncArrangor.execute(SyncArrangor(amtVirksomhet.organisasjonsnummer)).onLeft { err ->
            if (err is SyncArrangorError.FjernetAvJuridiskeArsaker) {
                logger.warn("Virksomhet med orgnr=${amtVirksomhet.organisasjonsnummer} er fjernet fra enhetsregisteret")
                return
            } else {
                logger.error("Feil ved sync av orgnr ${amtVirksomhet.organisasjonsnummer} fra enhetsregisteret")
                throw IllegalStateException("Forventet å finne virksomhet med orgnr ${amtVirksomhet.organisasjonsnummer} i enhetsregisteret. Er det feil data i meldingen? Error: $err")
            }
        }
    }

    private fun shouldIgnoreMessage(key: String): Boolean {
        return db.session { repository.arrangor.getByOrganisasjonsnummer(Organisasjonsnummer(key)) } == null
    }
}
