package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.FAGSYSTEM_HEADER_NAME
import no.nav.tiltak.okonomi.FakturaStatus
import no.nav.tiltak.okonomi.OkonomiFagsystem
import org.apache.kafka.clients.consumer.ConsumerRecord

class ReplikerFakturaStatusConsumer(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override fun consume(record: ConsumerRecord<String, JsonElement>): ConsumeStatus {
        val fagsystem = record.headers().lastHeader(FAGSYSTEM_HEADER_NAME).value().let { String(it) }
        if (fagsystem != OkonomiFagsystem.TILTAKSADMINISTRASJON.name) {
            return ConsumeStatus.OK
        }

        val (fakturanummer, status, fakturaStatusSistOppdatert) =
            JsonIgnoreUnknownKeys.decodeFromJsonElement<FakturaStatus>(record.value())

        db.transaction {
            utbetalingService.oppdaterFakturaStatus(fakturanummer.value, status, fakturaStatusSistOppdatert)
        }

        return ConsumeStatus.OK
    }
}
