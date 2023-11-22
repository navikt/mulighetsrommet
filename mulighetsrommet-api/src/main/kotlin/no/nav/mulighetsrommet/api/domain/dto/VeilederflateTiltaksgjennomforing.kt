package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class VeilederflateInnsatsgruppe(
    val sanityId: String,
    val tittel: String,
    val nokkel: String,
    val beskrivelse: String,
    val order: Int,
)

@Serializable
data class VeilederflateTiltaksgjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val sanityId: String? = null,
    val tiltakstype: VeilederflateTiltakstype,
    val navn: String,
    val stedForGjennomforing: String? = null,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus? = null,
    val tiltaksnummer: String? = null,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val arrangor: VeilederflateArrangor? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val kontaktinfoTiltaksansvarlige: List<KontaktinfoTiltaksansvarlige>,
    val fylke: String? = null,
    val enheter: List<String>? = emptyList(),
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
)

@Serializable
data class VeilederflateTiltakstype(
    val sanityId: String,
    val navn: String,
    val beskrivelse: String? = null,
    val innsatsgruppe: SanityInnsatsgruppe? = null,
    val regelverkLenker: List<RegelverkLenke>? = emptyList(),
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val arenakode: String? = null,
)

@Serializable
data class VeilederflateArrangor(
    val selskapsnavn: String?,
    val organisasjonsnummer: String?,
    val kontaktperson: Kontaktperson?,
) {
    @Serializable
    data class Kontaktperson(
        val navn: String,
        val telefon: String?,
        val epost: String,
    )
}

@Serializable
data class Oppskrifter(
    val data: List<Oppskrift>,
)

@Serializable
data class Oppskrift(
    val _id: String,
    val navn: String,
    val beskrivelse: String,
    val steg: List<JsonObject>,
    val _updatedAt: String,
)
