package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

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
    val sanityId: String,
    val tiltakstype: VeilederflateTiltakstype? = null,
    val navn: String,
    val stedForGjennomforing: String? = null,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus? = null,
    val tiltaksnummer: String? = null,
    val oppstart: TiltaksgjennomforingOppstartstype? = null,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val arrangor: VeilederflateArrangor? = null,
    val estimertVentetid: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val kontaktinfoTiltaksansvarlige: List<KontaktinfoTiltaksansvarlige>? = emptyList(),
    val fylke: String? = null,
    val enheter: List<String>? = emptyList(),
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
)

@Serializable
data class VeilederflateTiltakstype(
    val sanityId: String? = null,
    val navn: String? = null,
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
