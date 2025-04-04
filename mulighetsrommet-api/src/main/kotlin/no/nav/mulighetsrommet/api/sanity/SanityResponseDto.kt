package no.nav.mulighetsrommet.api.sanity

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.api.navansatt.SanityRedaktor
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class RegelverkLenke(
    val _id: String? = null,
    val beskrivelse: String? = null,
    val regelverkUrl: String? = null,
    val regelverkLenkeNavn: String? = null,
)

@Serializable
data class SanityTiltakstype(
    val _id: String,
    val tiltakstypeNavn: String,
    val beskrivelse: String? = null,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val regelverkLenker: List<RegelverkLenke>? = null,
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val kanKombineresMed: List<String> = emptyList(),
)

@Serializable
data class SanityTiltaksgjennomforing(
    val _id: String,
    val tiltakstype: SanityTiltakstype,
    val tiltaksgjennomforingNavn: String? = null,
    val tiltaksnummer: String? = null,
    val beskrivelse: String? = null,
    val stedForGjennomforing: String? = null,
    val fylke: NavEnhetNummer? = null,
    val enheter: List<NavEnhetNummer?>? = null,
    val arrangor: SanityArrangor? = null,
    val kontaktpersoner: List<SanityKontaktperson>? = null,
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val redaktor: List<SanityRedaktor>? = null,
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

@Serializable
data class KontaktinfoTiltaksansvarlig(
    val _rev: String? = null,
    val _type: String? = null,
    val navn: String? = null,
    val telefonnummer: String? = null,
    val _id: String? = null,
    val enhet: String? = null,
    val enhetsnummer: NavEnhetNummer? = null,
    val _updatedAt: String? = null,
    val _createdAt: String? = null,
    val epost: String? = null,
    val beskrivelse: String? = null,
    val navIdent: Slug? = null,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Slug(
    @EncodeDefault
    val _type: String = "slug",
    val current: String,
)

@Serializable
data class SanityKontaktperson(
    val navKontaktperson: KontaktinfoTiltaksansvarlig? = null,
    val enheter: List<String>,
)

@Serializable
data class EnhetRef(
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
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
