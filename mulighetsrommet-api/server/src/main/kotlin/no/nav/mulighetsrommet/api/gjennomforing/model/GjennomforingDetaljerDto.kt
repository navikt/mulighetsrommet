@file:UseSerializers(LocalDateSerializer::class, UUIDSerializer::class)

package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.tiltak.PrismodellDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface GjennomforingDetaljerDto

@Serializable
@SerialName("GjennomforingAvtaleDetaljerDto")
data class GjennomforingAvtaleDetaljerDto(
    val tiltakstype: Gjennomforing.Tiltakstype,
    val gjennomforing: GjennomforingAvtaleDto,
    val veilederinfo: GjennomforingVeilederinfoDto,
    val prismodell: PrismodellDto,
    val opplaring: OpplaringKategoriseringDetaljer?,
) : GjennomforingDetaljerDto

@Serializable
@SerialName("GjennomforingEnkeltplassDetaljerDto")
data class GjennomforingEnkeltplassDetaljerDto(
    val tiltakstype: Gjennomforing.Tiltakstype,
    val gjennomforing: GjennomforingEnkeltplassDto,
    val prismodell: PrismodellDto,
    val opplaring: OpplaringKategoriseringDetaljer?,
    val okonomi: TotrinnskontrollDto?,
    val prisendring: Prisendring?,
    val deltaker: DeltakerDto?,
) : GjennomforingDetaljerDto {

    @Serializable
    data class Prisendring(
        val totrinnskontroll: TotrinnskontrollDto,
        val prismodell: PrismodellDto,
    )
}

@Serializable
data class GjennomforingAvtaleDto(
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
    val tiltaksnummer: Tiltaksnummer?,
    val arrangor: GjennomforingDtoArrangor,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: DataElement.Status,
    val antallPlasser: Int,
    val avtaleId: UUID,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val apentForPamelding: Boolean,
    val deltidsprosent: Double,
    val tilgjengeligForArrangorDato: LocalDate?,
    val administratorer: List<Administrator>,
    val stengt: List<StengtPeriode>,
    val avbrytelse: AvbrytelseDto?,
) {

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class AvbrytelseDto(
        val aarsaker: List<AvbrytGjennomforingAarsak>,
        val forklaring: String?,
    )

    @Serializable
    data class StengtPeriode(
        val id: Int,
        val start: LocalDate,
        val slutt: LocalDate,
        val beskrivelse: String,
    )
}

@Serializable
data class GjennomforingEnkeltplassDto(
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
    val tiltaksnummer: Tiltaksnummer?,
    val arrangor: GjennomforingDtoArrangor,
    val startDato: LocalDate,
    val sluttDato: LocalDate,
    val status: DataElement.Status,
    val ansvarligEnhet: AnsvarligEnhet,
) {
    @Serializable
    data class AnsvarligEnhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}

@Serializable
data class GjennomforingVeilederinfoDto(
    val publisert: Boolean,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val kontorstruktur: List<Kontorstruktur>,
    val kontaktpersoner: List<GjennomforingKontaktpersonDto>,
    val oppmoteSted: String?,
    val estimertVentetid: EstimertVentetid?,
) {

    @Serializable
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )
}

@Serializable
data class GjennomforingKontaktpersonDto(
    val navIdent: NavIdent,
    val navn: String,
    val epost: String,
    val mobilnummer: String? = null,
    val hovedenhet: NavEnhetNummer,
    val beskrivelse: String?,
)

@Serializable
data class DeltakerDto(
    val id: UUID,
    val navn: String?,
    val norskIdent: NorskIdent?,
    val oppfolgingEnhet: NavEnhetDto?,
    val status: DataElement.Status,
    val innholdAnnet: String?,
    val avvistGrunn: AvvistGrunn?,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val navVeilederNavn: String?,
    val dagerPerUke: Float?,
) {
    companion object {
        fun from(deltaker: Deltaker, personalia: Personalia, navVeilederNavn: String?) = DeltakerDto(
            id = deltaker.id,
            status = deltaker.status.type.toDataElement(),
            innholdAnnet = deltaker.innholdAnnet,
            navn = personalia.navn(),
            norskIdent = personalia.norskIdent(),
            oppfolgingEnhet = personalia.oppfolgingEnhet(),
            avvistGrunn = personalia.avvistGrunn,
            startDato = deltaker.startDato,
            sluttDato = deltaker.sluttDato,
            navVeilederNavn = navVeilederNavn,
            dagerPerUke = deltaker.deltakelsesmengder.lastOrNull()?.dagerPerUke,
        )
    }
}

@Serializable
data class GjennomforingDtoArrangor(
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val slettet: Boolean,
    val kontaktpersoner: List<Kontaktperson> = listOf(),
) {

    @Serializable
    data class Kontaktperson(
        val id: UUID,
        val navn: String,
        val beskrivelse: String?,
        val telefon: String?,
        val epost: String,
    )
}
