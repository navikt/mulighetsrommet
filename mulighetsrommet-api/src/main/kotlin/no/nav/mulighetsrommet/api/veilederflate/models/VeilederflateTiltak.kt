package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.domain.dto.RegelverkLenke
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.PersonopplysningData
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
sealed class VeilederflateTiltak {
    abstract val tiltakstype: VeilederflateTiltakstype
    abstract val tittel: String
    abstract val underTittel: String
    abstract val status: TiltaksgjennomforingStatusDto
    abstract val beskrivelse: String?
    abstract val faneinnhold: Faneinnhold?
    abstract val kontaktinfo: VeilederflateKontaktinfo
    abstract val oppstart: TiltaksgjennomforingOppstartstype
    abstract val stedForGjennomforing: String?
    abstract val fylke: String
    abstract val enheter: List<String>
}

@Serializable
@SerialName("TILTAK_GRUPPE")
data class VeilederflateTiltakGruppe(
    override val tiltakstype: VeilederflateTiltakstype,
    override val tittel: String,
    override val underTittel: String,
    override val status: TiltaksgjennomforingStatusDto,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: TiltaksgjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylke: String,
    override val enheter: List<String>,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksnummer: String?,
    val apentForInnsok: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate?,
    val arrangor: VeilederflateArrangor,
    val estimertVentetid: EstimertVentetid?,
    val personvernBekreftet: Boolean,
    val personopplysningerSomKanBehandles: List<PersonopplysningData>,
) : VeilederflateTiltak()

@Serializable
@SerialName("TILTAK_ENKELTPLASS_ANSKAFFET")
data class VeilederflateTiltakEnkeltplassAnskaffet(
    override val tiltakstype: VeilederflateTiltakstype,
    override val tittel: String,
    override val underTittel: String,
    override val status: TiltaksgjennomforingStatusDto,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: TiltaksgjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylke: String,
    override val enheter: List<String>,
    val sanityId: String,
    val tiltaksnummer: String?,
    val arrangor: VeilederflateArrangor,
) : VeilederflateTiltak()

@Serializable
@SerialName("TILTAK_EGEN_REGI")
data class VeilederflateTiltakEgenRegi(
    override val tiltakstype: VeilederflateTiltakstype,
    override val tittel: String,
    override val underTittel: String,
    override val status: TiltaksgjennomforingStatusDto,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: TiltaksgjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylke: String,
    override val enheter: List<String>,
    val sanityId: String,
    val tiltaksnummer: String?,
) : VeilederflateTiltak()

@Serializable
@SerialName("TILTAK_ENKELTPLASS")
data class VeilederflateTiltakEnkeltplass(
    override val tiltakstype: VeilederflateTiltakstype,
    override val tittel: String,
    override val underTittel: String,
    override val status: TiltaksgjennomforingStatusDto,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: TiltaksgjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylke: String,
    override val enheter: List<String>,
    val sanityId: String,
) : VeilederflateTiltak()

@Serializable
data class VeilederflateKontaktinfoTiltaksansvarlig(
    val navn: String? = null,
    val telefon: String? = null,
    val enhet: NavEnhetDbo? = null,
    val epost: String? = null,
    val beskrivelse: String? = null,
)

@Serializable
data class VeilederflateKontaktinfo(
    val tiltaksansvarlige: List<VeilederflateKontaktinfoTiltaksansvarlig>,
)

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
