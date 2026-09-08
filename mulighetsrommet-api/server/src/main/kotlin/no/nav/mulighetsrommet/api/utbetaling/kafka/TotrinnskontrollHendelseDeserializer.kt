package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelseOld
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.common.serialization.Deserializer

/**
 * TODO: kan slettes når gamle meldinger har utgått
 * Totrinnskontroll-hendelser ble tidligere publisert med et annet skjema (se [TotrinnskontrollHendelseOld]).
 * Gammel deserialisering kan slettes når gamle meldinger ikke lengre finnes på topic (evt. v2 topic).
 */
class TotrinnskontrollHendelseDeserializer : Deserializer<TotrinnskontrollHendelse> {
    private val jsonElementDeserializer = JsonElementDeserializer()

    override fun deserialize(topic: String, data: ByteArray?): TotrinnskontrollHendelse {
        val message = jsonElementDeserializer.deserialize(topic, data)
        return try {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse>(message)
        } catch (_: Throwable) {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelseOld>(message).toNew()
        }
    }
}
