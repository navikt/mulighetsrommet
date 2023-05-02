package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class SanityTiltaksgjennomforingResponse(
    val tiltaksnummer: String?,
    val enheter: List<Enhet>?,
)

@Serializable
data class Enhet(
    val _ref: String?,
)

@Serializable
data class FylkeResponse(
    val fylke: Fylke,
)

@Serializable
data class Fylke(
    val nummer: Slug,
)

@Serializable
data class Slug(
    val current: String,
)

@Serializable(with = SanityReponseSerializer::class)
sealed class SanityResponse {
    @Serializable
    data class Result(
        val ms: Int,
        val query: String,
        val result: JsonElement,
    ) : SanityResponse() {
        inline fun <reified T> decode(): T {
            return Json.decodeFromJsonElement(result)
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
