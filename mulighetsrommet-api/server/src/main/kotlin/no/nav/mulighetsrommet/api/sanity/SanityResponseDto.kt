package no.nav.mulighetsrommet.api.sanity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class SanityTiltakstype(
    val _id: String,
    val tiltakstypeNavn: String? = null,
)

@Serializable
data class SanityArrangor(
    @Serializable(with = UUIDSerializer::class)
    val _id: UUID,
    val navn: String,
    val organisasjonsnummer: Organisasjonsnummer? = null,
    val kontaktpersoner: List<SanityArrangorKontaktperson>? = emptyList(),
)

@Serializable
data class SanityArrangorKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val _id: UUID,
    val navn: String,
    val telefon: String?,
    val epost: String,
    val beskrivelse: String?,
)

@Serializable(with = SanityReponseSerializer::class)
sealed class SanityResponse {
    @Serializable
    data class Result(
        val ms: Int,
        val query: String,
        val result: JsonElement?,
    ) : SanityResponse() {
        inline fun <reified T> decode(): T {
            if (result == null) {
                return null as T
            }
            return JsonIgnoreUnknownKeys.decodeFromJsonElement(result)
        }
    }

    @Serializable
    data class Error(
        val error: JsonObject,
    ) : SanityResponse()
}

object SanityReponseSerializer : JsonContentPolymorphicSerializer<SanityResponse>(SanityResponse::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "result" in element.jsonObject -> SanityResponse.Result.serializer()
        else -> SanityResponse.Error.serializer()
    }
}
