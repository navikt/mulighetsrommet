package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatusDto
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class VeilederflateInnsatsgruppe(
    val tittel: String,
    val nokkel: String,
    val order: Int,
)

@Serializable
data class VeilederflateTiltaksgjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val status: TiltaksgjennomforingStatusDto,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
    val sanityId: String? = null,
    val tiltakstype: VeilederflateTiltakstype,
    val navn: String,
    val stedForGjennomforing: String? = null,
    val apentForInnsok: Boolean,
    val tiltaksnummer: String? = null,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val arrangor: VeilederflateArrangor? = null,
    val fylke: String? = null,
    val enheter: List<String>? = emptyList(),
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
    val kontaktinfo: VeilederflateKontaktinfo? = null,
    val estimertVentetid: EstimertVentetid? = null,
    val personvernBekreftet: Boolean,
)

@Serializable
data class VeilederflateKontaktinfoTiltaksansvarlig(
    val navn: String? = null,
    val telefonnummer: String? = null,
    val enhet: NavEnhetDbo? = null,
    val epost: String? = null,
    val beskrivelse: String? = null,
)

@Serializable
data class VeilederflateKontaktinfo(
    val varsler: List<KontaktinfoVarsel>,
    val tiltaksansvarlige: List<VeilederflateKontaktinfoTiltaksansvarlig>,
)

enum class KontaktinfoVarsel {
    IKKE_TILGANG_TIL_KONTAKTINFO,
}

@Serializable
data class VeilederflateTiltakstype(
    val sanityId: String,
    val navn: String,
    val beskrivelse: String? = null,
    val innsatsgrupper: Set<Innsatsgruppe>? = null,
    val regelverkLenker: List<RegelverkLenke>? = emptyList(),
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val arenakode: String? = null,
    val tiltakskode: Tiltakskode? = null,
    val kanKombineresMed: List<String> = emptyList(),
)

@Serializable
data class VeilederflateArrangor(
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID,
    val selskapsnavn: String?,
    val organisasjonsnummer: String?,
    val kontaktpersoner: List<VeilederflateArrangorKontaktperson>,
)

@Serializable
data class VeilederflateArrangorKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val epost: String,
    val telefon: String?,
    val beskrivelse: String?,
)

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

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: String,
)
