package no.nav.tiltak.okonomi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mapping fra FQCN til eksplisitt @SerialName.
 */
private val legacyTypeDiscriminators = mapOf(
    "no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt" to "NAV_ANSATT",
    "no.nav.tiltak.okonomi.OkonomiPart.System" to "FAGSYSTEM",
    "no.nav.tiltak.okonomi.OpprettBestilling.Arrangor.Norsk" to "NORSK",
    "no.nav.tiltak.okonomi.OpprettBestilling.Arrangor.Utenlandsk" to "UTENLANDSK",
    "no.nav.tiltak.okonomi.OpprettFaktura.Betalingsinformasjon.BBan" to "BBAN",
    "no.nav.tiltak.okonomi.OpprettFaktura.Betalingsinformasjon.IBan" to "IBAN",
)

private val bestillingMeldingJson = Json {
    ignoreUnknownKeys = true
}

private fun JsonElement.migrateLegacyTypeDiscriminators(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        mapValues { (key, value) ->
            if (key == "type" && value is JsonPrimitive && value.isString) {
                legacyTypeDiscriminators[value.jsonPrimitive.content]?.let { JsonPrimitive(it) } ?: value
            } else {
                value.migrateLegacyTypeDiscriminators()
            }
        },
    )

    is JsonArray -> JsonArray(map { it.migrateLegacyTypeDiscriminators() })

    else -> this
}

/**
 * Dekoder en [OkonomiBestillingMelding] fra "bestillinger"-topicet. Oversetter gammelt format til nytt.
 *
 * TODO: fjern denne oversettelsen når det ikke lenger finnes meldinger i det gamle formatet på "bestillinger"-topicet.
 */
fun decodeOkonomiBestillingMelding(message: JsonElement): OkonomiBestillingMelding = bestillingMeldingJson.decodeFromJsonElement(message.migrateLegacyTypeDiscriminators())
