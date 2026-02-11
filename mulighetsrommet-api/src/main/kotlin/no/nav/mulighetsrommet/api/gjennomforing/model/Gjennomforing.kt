package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale.ArrangorKontaktperson
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
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

sealed class Gjennomforing {
    abstract val id: UUID
    abstract val lopenummer: Tiltaksnummer
    abstract val tiltakstype: Tiltakstype
    abstract val arrangor: ArrangorUnderenhet
    abstract val arena: ArenaData?
    abstract val navn: String
    abstract val startDato: LocalDate
    abstract val sluttDato: LocalDate?
    abstract val deltidsprosent: Double
    abstract val antallPlasser: Int
    abstract val opphav: ArenaMigrering.Opphav
    abstract val opprettetTidspunkt: Instant
    abstract val oppdatertTidspunkt: Instant

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val kontaktpersoner: List<ArrangorKontaktperson>,
        val slettet: Boolean,
    )

    @Serializable
    data class ArenaData(
        val tiltaksnummer: Tiltaksnummer?,
        val ansvarligNavEnhet: String?,
    )
}

@Serializable
data class GjennomforingAvtale(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val tiltakstype: Tiltakstype,
    override val lopenummer: Tiltaksnummer,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    override val sluttDato: LocalDate?,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opphav: ArenaMigrering.Opphav,
    @Serializable(with = InstantSerializer::class)
    override val opprettetTidspunkt: Instant,
    @Serializable(with = InstantSerializer::class)
    override val oppdatertTidspunkt: Instant,
    val status: GjennomforingStatus,
    val apentForPamelding: Boolean,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val administratorer: List<Administrator>,
    val kontorstruktur: List<Kontorstruktur>,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val kontaktpersoner: List<GjennomforingKontaktperson>,
    val oppmoteSted: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val publisert: Boolean,
    val estimertVentetid: EstimertVentetid?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDto?,
    val stengt: List<StengtPeriode>,
    val prismodell: Prismodell,
) : Gjennomforing() {

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class ArrangorKontaktperson(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val beskrivelse: String?,
        val telefon: String?,
        val epost: String,
    )

    @Serializable
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )

    @Serializable
    data class StengtPeriode(
        val id: Int,
        @Serializable(with = LocalDateSerializer::class)
        val start: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val slutt: LocalDate,
        val beskrivelse: String,
    )
}

@Serializable
data class GjennomforingEnkeltplass(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    override val sluttDato: LocalDate?,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opphav: ArenaMigrering.Opphav,
    @Serializable(with = InstantSerializer::class)
    override val opprettetTidspunkt: Instant,
    @Serializable(with = InstantSerializer::class)
    override val oppdatertTidspunkt: Instant,
    val status: GjennomforingStatusType,
) : Gjennomforing()

@Serializable
data class GjennomforingArena(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val arena: ArenaData?,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    override val sluttDato: LocalDate?,
    override val deltidsprosent: Double,
    override val antallPlasser: Int,
    override val opphav: ArenaMigrering.Opphav,
    @Serializable(with = InstantSerializer::class)
    override val opprettetTidspunkt: Instant,
    @Serializable(with = InstantSerializer::class)
    override val oppdatertTidspunkt: Instant,
    val status: GjennomforingStatusType,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
) : Gjennomforing()
