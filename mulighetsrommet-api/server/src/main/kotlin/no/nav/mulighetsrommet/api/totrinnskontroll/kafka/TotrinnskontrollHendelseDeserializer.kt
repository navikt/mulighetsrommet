package no.nav.mulighetsrommet.api.totrinnskontroll.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelseOld
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.common.serialization.Deserializer
import java.nio.charset.StandardCharsets

/**
 * Håndterer mapping fra gammelt format på hendelser til nytt format.
 * TODO: deserializer for gammelt format kan fjernes rundt nyttår
 */
class TotrinnskontrollHendelseDeserializer : Deserializer<TotrinnskontrollHendelse> {
    override fun deserialize(topic: String, data: ByteArray): TotrinnskontrollHendelse {
        val json = Json.parseToJsonElement(String(data, StandardCharsets.UTF_8))

        return try {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse>(json)
        } catch (_: Throwable) {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelseOld>(json).toNew()
        }
    }
}
