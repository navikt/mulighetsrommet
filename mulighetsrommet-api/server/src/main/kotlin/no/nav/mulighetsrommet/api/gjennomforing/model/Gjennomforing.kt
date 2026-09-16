@file:UseSerializers(UUIDSerializer::class, LocalDateSerializer::class, InstantSerializer::class)

package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class Gjennomforing {
    abstract val id: UUID
    abstract val lopenummer: Tiltaksnummer
    abstract val tiltakstype: Tiltakstype
    abstract val arrangor: ArrangorUnderenhet
    abstract val arena: ArenaData?
    abstract val navn: String
    abstract val startDato: LocalDate?
    abstract val sluttDato: LocalDate?
    abstract val deltidsprosent: Double
    abstract val antallPlasser: Int
    abstract val opprettetTidspunkt: Instant
    abstract val oppdatertTidspunkt: Instant
    abstract val oppstart: GjennomforingOppstartstype
    abstract val pameldingType: GjennomforingPameldingType

    @Serializable
    data class Tiltakstype(
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArrangorUnderenhet(
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class ArenaData(
        val tiltaksnummer: Tiltaksnummer,
        val ansvarligNavEnhet: String?,
    )
}

@Serializable
sealed class GjennomforingTiltaksadministrasjon : Gjennomforing() {
    abstract val prismodell: Prismodell
}

@Serializable
data class GjennomforingAvtaleDetaljer(
    val publisert: Boolean,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val kontorstruktur: List<Kontorstruktur>,
    val kontaktpersoner: List<GjennomforingKontaktperson>,
    val oppmoteSted: String?,
    val estimertVentetid: EstimertVentetid?,
    val administratorer: List<Administrator>,
    val opplaringKategorisering: OpplaringKategoriseringDetaljer?,
    val tilgjengeligForArrangorDato: LocalDate?,
    val arrangorKontaktpersoner: List<ArrangorKontaktperson>,
) {
    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class GjennomforingKontaktperson(
        val navIdent: NavIdent,
        val navn: String,
        val epost: String,
        val mobilnummer: String? = null,
        val hovedenhet: NavEnhetNummer,
        val beskrivelse: String?,
    )

    @Serializable
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )

    @Serializable
    data class ArrangorKontaktperson(
        val id: UUID,
        val navn: String,
        val beskrivelse: String?,
        val telefon: String?,
        val epost: String,
    )
}

@Serializable
data class GjennomforingAvtale(
    override val id: UUID,
    override val tiltakstype: Tiltakstype,
    override val lopenummer: Tiltaksnummer,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    val status: GjennomforingAvtaleStatus,
    override val startDato: LocalDate,
    override val sluttDato: LocalDate?,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opprettetTidspunkt: Instant,
    override val oppdatertTidspunkt: Instant,
    override val prismodell: Prismodell,
    override val oppstart: GjennomforingOppstartstype,
    override val pameldingType: GjennomforingPameldingType,
    val avtaleId: UUID,
    val kontorstruktur: List<Kontorstruktur>,
    val apentForPamelding: Boolean,
    val stengt: List<StengtPeriode>,
) : GjennomforingTiltaksadministrasjon() {
    @Serializable
    data class StengtPeriode(
        val id: Int,
        val start: LocalDate,
        val slutt: LocalDate,
        val beskrivelse: String,
    )
}

@Serializable
data class GjennomforingEnkeltplass(
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    val status: GjennomforingEnkeltplassStatus,
    override val startDato: LocalDate,
    override val sluttDato: LocalDate,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opprettetTidspunkt: Instant,
    override val oppdatertTidspunkt: Instant,
    override val prismodell: Prismodell,
    override val oppstart: GjennomforingOppstartstype,
    override val pameldingType: GjennomforingPameldingType,
    val ansvarligEnhet: AnsvarligEnhet,
) : GjennomforingTiltaksadministrasjon() {

    init {
        check(oppstart == GjennomforingOppstartstype.ENKELTPLASS) {
            "oppstart må være satt til ${GjennomforingOppstartstype.ENKELTPLASS} for enkeltplasser"
        }
    }

    @Serializable
    data class AnsvarligEnhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}

@Serializable
data class GjennomforingArena(
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    val status: GjennomforingAvtaleStatus,
    override val startDato: LocalDate,
    override val sluttDato: LocalDate?,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opprettetTidspunkt: Instant,
    override val oppdatertTidspunkt: Instant,
    override val oppstart: GjennomforingOppstartstype,
    override val pameldingType: GjennomforingPameldingType,
) : Gjennomforing()
