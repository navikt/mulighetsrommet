package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.sanity.RegelverkLenke
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class VeilederflateInnsatsgruppe(
    val tittel: String,
    val nokkel: Innsatsgruppe,
    val order: Int,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class VeilederflateTiltak {
    abstract val tiltakstype: VeilederflateTiltakstype
    abstract val navn: String
    abstract val beskrivelse: String?
    abstract val faneinnhold: Faneinnhold?
    abstract val kontaktinfo: VeilederflateKontaktinfo
    abstract val oppstart: GjennomforingOppstartstype
    abstract val stedForGjennomforing: String?
    abstract val fylker: List<NavEnhetNummer>
    abstract val enheter: List<NavEnhetNummer>
}

@Serializable
data class VeilederflateTiltakGruppe(
    override val tiltakstype: VeilederflateTiltakstype,
    override val navn: String,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: GjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylker: List<NavEnhetNummer>,
    override val enheter: List<NavEnhetNummer>,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: VeilederflateTiltakGruppeStatus,
    val tiltaksnummer: String?,
    val apentForPamelding: Boolean,
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
data class VeilederflateTiltakGruppeStatus(
    val type: GjennomforingStatus,
    val beskrivelse: String,
)

@Serializable
data class VeilederflateTiltakEnkeltplassAnskaffet(
    override val tiltakstype: VeilederflateTiltakstype,
    override val navn: String,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: GjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylker: List<NavEnhetNummer>,
    override val enheter: List<NavEnhetNummer>,
    val sanityId: String,
    val tiltaksnummer: String?,
    val arrangor: VeilederflateArrangor,
) : VeilederflateTiltak()

@Serializable
data class VeilederflateTiltakEgenRegi(
    override val tiltakstype: VeilederflateTiltakstype,
    override val navn: String,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: GjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylker: List<NavEnhetNummer>,
    override val enheter: List<NavEnhetNummer>,
    val sanityId: String,
    val tiltaksnummer: String?,
) : VeilederflateTiltak()

@Serializable
data class VeilederflateTiltakEnkeltplass(
    override val tiltakstype: VeilederflateTiltakstype,
    override val navn: String,
    override val beskrivelse: String?,
    override val faneinnhold: Faneinnhold?,
    override val kontaktinfo: VeilederflateKontaktinfo,
    override val oppstart: GjennomforingOppstartstype,
    override val stedForGjennomforing: String?,
    override val fylker: List<NavEnhetNummer>,
    override val enheter: List<NavEnhetNummer>,
    val sanityId: String,
) : VeilederflateTiltak()

@Serializable
data class VeilederflateKontaktinfoTiltaksansvarlig(
    val navn: String? = null,
    val telefon: String? = null,
    val enhet: VeilederflateTiltaksansvarligHovedenhet? = null,
    val epost: String? = null,
    val beskrivelse: String? = null,
)

@Serializable
data class VeilederflateTiltaksansvarligHovedenhet(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
)

@Serializable
data class VeilederflateKontaktinfo(
    val tiltaksansvarlige: List<VeilederflateKontaktinfoTiltaksansvarlig>,
)

@Serializable
data class VeilederflateTiltakstype(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
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
    val steg: List<OppskriftSteg>,
    val _updatedAt: String,
)

@Serializable
data class OppskriftSteg(
    val _type: String,
    val _key: String,
    val navn: String,
    val innhold: List<PortableTextTypedObject>,
)

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: String,
)

@Serializable
data class VeilederflateNavEnhet(
    val enhetsnummer: NavEnhetNummer,
    val type: Norg2Type,
)
