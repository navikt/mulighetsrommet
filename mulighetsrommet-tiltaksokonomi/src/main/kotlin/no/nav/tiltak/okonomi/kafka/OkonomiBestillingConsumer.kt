package no.nav.tiltak.okonomi.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.service.OkonomiService
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class OkonomiBestillingConsumer(
    private val okonomi: OkonomiService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        MDC.put("bestillingsnummer", key)

        try {
            behandleMelding(
                bestillingsnummer = key,
                melding = Json.decodeFromJsonElement<OkonomiBestillingMelding>(message),
            )
        } finally {
            MDC.clear()
        }
    }

    private suspend fun behandleMelding(bestillingsnummer: String, melding: OkonomiBestillingMelding) {
        logger.info("Behandler melding for bestilling=$bestillingsnummer")

        val result = when (melding) {
            is OkonomiBestillingMelding.Bestilling -> {
                logger.info("Oppretter bestilling=$bestillingsnummer")
                okonomi.opprettBestilling(melding.payload)
            }

            is OkonomiBestillingMelding.Annullering -> {
                logger.info("Annullerer bestilling=$bestillingsnummer")
                okonomi.annullerBestilling(melding.payload)
            }

            is OkonomiBestillingMelding.Faktura -> {
                logger.info("Oppretter faktura for bestilling=$bestillingsnummer")
                okonomi.opprettFaktura(melding.payload)
            }

            is OkonomiBestillingMelding.GjorOppBestilling -> {
                logger.info("Gjør opp bestilling=$bestillingsnummer")
                okonomi.gjorOppBestilling(melding.payload)
            }
        }

        result.onRight {
            logger.info("Melding for bestilling=$bestillingsnummer behandlet")
        }.onLeft {
            logger.error("Feil ved behandling av melding for bestilling=$bestillingsnummer", it)
            throw it
        }
    }
}
