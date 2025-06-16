package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.FakturaStatus
import org.slf4j.LoggerFactory

class ReplicateOkonomiFakturaStatus(
    private val db: ApiDatabase,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer statusmelding fakturanummer=$key")

        val (fakturanummer, status, fakturaStatusSistOppdatert) =
            JsonIgnoreUnknownKeys.decodeFromJsonElement<FakturaStatus>(message)

        queries.delutbetaling.setFakturaStatus(fakturanummer, status, fakturaStatusSistOppdatert)
    }
}
