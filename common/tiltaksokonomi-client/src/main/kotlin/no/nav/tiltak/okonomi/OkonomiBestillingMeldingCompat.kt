package no.nav.tiltak.okonomi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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

private fun JsonElement.migrateLegacyBestillingFields(): JsonElement {
    val message = jsonObject
    if (message["type"]?.jsonPrimitive?.content != "BESTILLING") return this

    val payload = message.getValue("payload").jsonObject.toMutableMap()
    payload.remove("tilskuddstype")?.let { payload.putIfAbsent("regnskapskonto", it) }
    payload.remove("tiltakskode")?.let { payload.putIfAbsent("artskonto", it) }
    return JsonObject(message + ("payload" to JsonObject(payload)))
}

private fun JsonElement.migrateBetalingReferanseFields(): JsonElement {
    val message = jsonObject
    if (message["type"]?.jsonPrimitive?.content != "FAKTURA") return this

    val payload = message.getValue("payload").jsonObject.toMutableMap()
    val betalingsinformasjon = payload["betalingsinformasjon"]?.jsonObject?.toMutableMap() ?: return this
    if (betalingsinformasjon["type"]?.jsonPrimitive?.content != "BBAN") return this
    if ("referanse" in betalingsinformasjon) return this

    val kid = betalingsinformasjon.remove("kid") ?: return this
    val referanse = if (kid == kotlinx.serialization.json.JsonNull) {
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("MeldingTilMottaker"),
                "melding" to payload.getValue("beskrivelse"),
            ),
        )
    } else {
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("KidNummer"),
                "kid" to kid,
            ),
        )
    }

    betalingsinformasjon["referanse"] = referanse
    payload["betalingsinformasjon"] = JsonObject(betalingsinformasjon)
    return JsonObject(message + ("payload" to JsonObject(payload)))
}

/**
 * Dekoder en [OkonomiBestillingMelding] fra "bestillinger"-topicet. Oversetter gammelt format til nytt.
 *
 * TODO: fjern denne oversettelsen når det ikke lenger finnes meldinger i det gamle formatet på "bestillinger"-topicet.
 */
fun decodeOkonomiBestillingMelding(message: JsonElement): OkonomiBestillingMelding = bestillingMeldingJson.decodeFromJsonElement(
    message.migrateLegacyTypeDiscriminators().migrateLegacyBestillingFields().migrateBetalingReferanseFields(),
)
