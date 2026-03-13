package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.UUID

class HelvedStatusV1KafkaConsumer(private val db: ApiDatabase) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    companion object {
        private const val FAGSYSTEM_HEADER_NAME = "fagsystem"
        private const val EXPECTED_FAGSYSTEM = "TILTAKSADMINISTRASJON"
    }
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Statusmeldingene på helved.status.v1 inneholder en Kafka-header med nøkkelen fagsystem. Denne brukes for å filtrere ut relevante statuser for det aktuelle fagsystemet.
     */
    override fun consume(record: ConsumerRecord<UUID, JsonElement>): ConsumeStatus {
        val fagsystem = record.headers().lastHeader(FAGSYSTEM_HEADER_NAME).value().let { String(it) }
        if (fagsystem == EXPECTED_FAGSYSTEM) {
            return super.consume(record)
        }
        logger.info("Mottok status-melding for fagsystem=$fagsystem, som ikke er relevant for oss. Ignorerer meldingen.")
        return ConsumeStatus.OK
    }

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer utbetaling status-melding med id=$key")
        val helvedStatus = JsonIgnoreUnknownKeys.decodeFromJsonElement<HelVedStatus>(message)
        when (helvedStatus.status) {
            HelVedStatus.Status.MOTTATT -> TODO()
            HelVedStatus.Status.FEILET -> TODO()
            HelVedStatus.Status.HOS_OPPDRAG -> TODO()
            HelVedStatus.Status.OK -> TODO()
        }
    }
}
