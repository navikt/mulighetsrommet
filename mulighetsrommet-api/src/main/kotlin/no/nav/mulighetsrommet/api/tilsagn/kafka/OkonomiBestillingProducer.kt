package no.nav.mulighetsrommet.api.tilsagn.kafka

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.OpprettFaktura
import org.apache.kafka.clients.producer.ProducerRecord

class OkonomiBestillingProducer(
    private val kafkaProducerClient: KafkaProducerClient<String, String?>,
    private val config: Config,
) {
    data class Config(
        val topic: String,
    )

    fun publishBestilling(bestilling: OpprettBestilling) {
        val message = OkonomiBestillingMelding.Bestilling(bestilling)
        publish(bestilling.bestillingsnummer, message)
    }

    fun publishAnnullering(bestillingsnummer: String) {
        publish(bestillingsnummer, OkonomiBestillingMelding.Annullering)
    }

    fun publishFaktura(faktura: OpprettFaktura) {
        val message = OkonomiBestillingMelding.Faktura(faktura)
        publish(faktura.bestillingsnummer, message)
    }

    private fun publish(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            bestillingsnummer,
            Json.encodeToString(message),
        )
        kafkaProducerClient.sendSync(record)
    }
}
