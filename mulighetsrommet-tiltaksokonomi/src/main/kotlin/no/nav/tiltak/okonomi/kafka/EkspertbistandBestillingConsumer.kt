package no.nav.tiltak.okonomi.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.mulighetsrommet.kafka.ScheduledMessageKafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementSerde
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.service.TiltaksokonomiService
import org.apache.kafka.common.serialization.Serdes

/**
 * Mottar meldinger på bestillinger-topicet til Ekspertbistand.
 */
class EkspertbistandBestillingConsumer(
    kafkaConsumerRepository: KafkaConsumerRepository,
    okonomi: TiltaksokonomiService,
) : ScheduledMessageKafkaTopicConsumer<String, JsonElement>(
    kafkaConsumerRepository,
    Serdes.StringSerde(),
    JsonElementSerde(),
) {
    private val handler = BestillingMeldingHandler(okonomi)

    override suspend fun consume(key: String, message: JsonElement) {
        val bestillingsnummer = Bestillingsnummer(key)
        require(bestillingsnummer.value.startsWith("E-")) {
            "Ugyldig bestillingsnummer=$bestillingsnummer fra Ekspertbistand"
        }

        val melding = Json.decodeFromJsonElement<OkonomiBestillingMelding>(message)
        when {
            melding is OkonomiBestillingMelding.Bestilling -> {
                require(melding.payload.tiltakskode.system == TiltakstypeSystem.EKSPERTBISTAND) {
                    "Ugyldig tiltakskode ${melding.payload.tiltakskode} fra Ekspertbistand"
                }
            }
        }

        handler.handle(OkonomiFagsystem.EKSPERTBISTAND, bestillingsnummer, melding)
    }
}
