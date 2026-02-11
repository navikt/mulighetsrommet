package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.PrismodellDto
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class GjennomforingDetaljerDto(
    val tiltakstype: Gjennomforing.Tiltakstype,
    val gjennomforing: GjennomforingDto,
    val veilederinfo: GjennomforingVeilederinfoDto?,
    val prismodell: PrismodellDto?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDto?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class GjennomforingDto {
    @Serializable
    data class Status(
        val type: GjennomforingStatusType,
        val status: DataElement.Status,
    )
}

@Serializable
@SerialName("GjennomforingAvtaleDto")
data class GjennomforingAvtaleDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
    val tiltaksnummer: Tiltaksnummer?,
    val arrangor: Gjennomforing.ArrangorUnderenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: Status,
    val antallPlasser: Int,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val opphav: ArenaMigrering.Opphav,
    val apentForPamelding: Boolean,
    val deltidsprosent: Double,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val administratorer: List<GjennomforingAvtale.Administrator>,
    val stengt: List<GjennomforingAvtale.StengtPeriode>,
) : GjennomforingDto()

@Serializable
@SerialName("GjennomforingEnkeltplassDto")
data class GjennomforingEnkeltplassDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
    val tiltaksnummer: Tiltaksnummer?,
    val arrangor: Gjennomforing.ArrangorUnderenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: Status,
    val opphav: ArenaMigrering.Opphav,
) : GjennomforingDto()

@Serializable
data class GjennomforingVeilederinfoDto(
    val publisert: Boolean,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val kontorstruktur: List<Kontorstruktur>,
    val kontaktpersoner: List<GjennomforingKontaktperson>,
    val oppmoteSted: String?,
    val estimertVentetid: GjennomforingAvtale.EstimertVentetid?,
)
