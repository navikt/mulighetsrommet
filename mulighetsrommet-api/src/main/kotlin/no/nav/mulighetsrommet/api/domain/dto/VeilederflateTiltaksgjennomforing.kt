package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class VeilederflateTiltaksgjennomforing(
    val _id: String, // TODO sanityId
    val tiltakstype: VeilederflateTiltakstype? = null,
    val tiltaksgjennomforingNavn: String, // TODO navn
    val lokasjon: String? = null,
    val tilgjengelighetsstatus: TiltaksgjennomforingTilgjengelighetsstatus? = null, // TODO tilgjengelighet
    val tiltaksnummer: String? = null,
    val oppstart: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val kontaktinfoArrangor: VeilederflateArrangor? = null,
    val estimert_ventetid: String? = null, // TODO estimertVentetid
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val kontaktinfoTiltaksansvarlige: List<KontaktinfoTiltaksansvarlige>? = emptyList(),
    val fylke: FylkeRef? = null,
    val enheter: List<EnhetRef>? = emptyList(),
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
)

@Serializable
data class VeilederflateTiltakstype(
    val _id: String? = null,
    val tiltakstypeNavn: String? = null,
    val beskrivelse: String? = null,
    val nokkelinfoKomponenter: List<NokkelinfoKomponent>? = emptyList(),
    val innsatsgruppe: Innsatsgruppe? = null,
    val regelverkLenker: List<RegelverkLenke>? = emptyList(),
    val faneinnhold: Faneinnhold? = null,
    val delingMedBruker: String? = null,
    val arenakode: String? = null,
)

@Serializable
data class VeilederflateArrangor(
    val selskapsnavn: String?,
    val organisasjonsnummer: String?,
    val lokasjon: String?,
    val kontaktperson: Kontaktperson?,
) {
    @Serializable
    data class Kontaktperson(
        val navn: String,
        val telefon: String?,
        val epost: String,
    )
}
