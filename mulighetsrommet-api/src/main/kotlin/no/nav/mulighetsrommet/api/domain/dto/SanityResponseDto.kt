package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class SanityTiltaksgjennomforingResponse(
    val _id: String,
    val tiltaksgjennomforingNavn: String,
    val enheter: List<EnhetRef>? = null,
    val lokasjon: String? = null,
    val tilgjengelighetsstatus: String? = null,
    val tiltakstype: TiltakstypeRef? = null,
    val fylkeRef: FylkeRef? = null,
    val tiltaksnummer: TiltaksnummerSlug? = null,
)

@Serializable
data class EnhetRef(
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)

@Serializable
data class TiltaksnummerSlug(
    val current: String,
    val _type: String = "slug",
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
