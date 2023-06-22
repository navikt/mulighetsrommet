package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.time.LocalDate

@Serializable
data class Faneinnhold(
    val forHvem: List<Innhold>? = emptyList(),
    val detaljerOgInnhold: List<Innhold>? = emptyList(),
    val pameldingOgVarighet: List<Innhold>? = emptyList(),
)

@Serializable
data class Innhold(
    val style: String? = null,
    val _key: String? = null,
    val level: Int? = null,
    val listItem: String? = null,
    val _type: String? = null,
    val children: List<InnholdChild>? = emptyList(),
)

@Serializable
data class InnholdChild(
    val text: String? = null,
    val _type: String? = null,
)

@Serializable
data class RegelverkLenke(
    val _id: String? = null,
    val beskrivelse: String? = null,
    val regelverkUrl: String? = null,
    val regelverkLenkeNavn: String? = null,
)

@Serializable
data class NokkelinfoKomponent(
    val hjelpetekst: String? = null,
    val innhold: String? = null,
    val tittel: String? = null,
)

@Serializable
data class Innsatsgruppe(
    val tittel: String? = null,
    val nokkel: String? = null,
    val beskrivelse: String? = null,
)

@Serializable
data class Tiltakstype(
    val _id: String? = null,
    val beskrivelse: String? = null,
    val regelverkLenker: List<RegelverkLenke>? = emptyList(),
    val regelverkFiler: List<String>? = emptyList(),
    val nokkelinfoKomponenter: List<NokkelinfoKomponent>? = emptyList(),
    val innsatsgruppe: Innsatsgruppe? = null,
    val tiltaksgruppe: String? = null,
    val tiltakstypeNavn: String? = null,
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val arenakode: String? = null,
)

@Serializable
data class VeilederflateTiltaksgjennomforing(
    val _id: String,
    val tiltaksgjennomforingNavn: String,
    val lokasjon: String? = null,
    val tilgjengelighetsstatus: String? = null,
    val tiltakstype: Tiltakstype? = null,
    val tiltaksnummer: String? = null,
    val oppstart: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    val kontaktInfoArrangor: KontaktInfoArrangor? = null,
    val estimert_ventetid: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val delingMedBruker: String? = null,
    val varighet: String? = null,
    val pameldingOgVarighet: List<PameldingOgVarighet> = emptyList(),
    val kontaktinfoTiltaksansvarlige: List<KontaktinfoTiltaksansvarlige> = emptyList(),
    val faneinnhold: Faneinnhold? = null,
)

@Serializable
data class KontaktinfoTiltaksansvarlige(
    val _rev: String? = null,
    val _type: String? = null,
    val navn: String? = null,
    val telefonnummer: String? = null,
    val _id: String? = null,
    val enhet: String? = null,
    val _updatedAt: String? = null,
    val _createdAt: String? = null,
    val epost: String? = null,
)

@Serializable
data class PameldingOgVarighet(
    val selskapsnavn: String? = null,
)

@Serializable
data class KontaktInfoArrangor(
    val selskapsnavn: String? = null,
    val _id: String? = null,
    val _updatedAt: String? = null,
    val _createdAt: String? = null,
    val _rev: String? = null,
    val _type: String? = null,
    val adresse: String? = null,
    val telefonnummer: String? = null,
)

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
