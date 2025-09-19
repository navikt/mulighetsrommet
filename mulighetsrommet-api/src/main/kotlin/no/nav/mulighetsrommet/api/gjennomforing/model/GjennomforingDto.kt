package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class GjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Gjennomforing.Tiltakstype,
    val navn: String,
    val tiltaksnummer: String?,
    val arrangor: Gjennomforing.ArrangorUnderenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val status: Status,
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val administratorer: List<Gjennomforing.Administrator>,
    val kontorstruktur: List<Kontorstruktur>,
    val oppstart: GjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val kontaktpersoner: List<GjennomforingKontaktperson>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val publisert: Boolean,
    val deltidsprosent: Double,
    val estimertVentetid: LabeledDataElement?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDto?,
    val stengt: List<Gjennomforing.StengtPeriode>,
) {
    @Serializable
    data class Status(
        val type: GjennomforingStatusType,
        val status: DataElement.Status,
    )
}
