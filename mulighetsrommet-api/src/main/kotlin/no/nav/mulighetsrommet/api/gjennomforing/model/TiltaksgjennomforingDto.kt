package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltaksgjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val tiltaksnummer: String?,
    val arrangor: ArrangorUnderenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val status: TiltaksgjennomforingStatusDto,
    val apentForInnsok: Boolean,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val administratorer: List<Administrator>,
    val navRegion: NavEnhetDbo?,
    val navEnheter: List<NavEnhetDbo>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktperson>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val publisert: Boolean,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDto?,
) {
    fun isAktiv(): Boolean = status.status in listOf(
        TiltaksgjennomforingStatus.PLANLAGT,
        TiltaksgjennomforingStatus.GJENNOMFORES,
    )

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
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
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )

    fun toTiltaksgjennomforingV1Dto() =
        TiltaksgjennomforingEksternV1Dto(
            id = id,
            tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
                id = tiltakstype.id,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode.toArenaKode(),
                tiltakskode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            startDato = startDato,
            sluttDato = sluttDato,
            status = status.status,
            virksomhetsnummer = arrangor.organisasjonsnummer.value,
            oppstart = oppstart,
            tilgjengeligForArrangorFraOgMedDato = tilgjengeligForArrangorFraOgMedDato,
            apentForInnsok = apentForInnsok,
        )

    fun toTiltaksgjennomforingDbo() =
        TiltaksgjennomforingDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            arrangorId = arrangor.id,
            arrangorKontaktpersoner = arrangor.kontaktpersoner.map { it.id },
            startDato = startDato,
            sluttDato = sluttDato,
            apentForInnsok = apentForInnsok,
            antallPlasser = antallPlasser ?: -1,
            avtaleId = avtaleId ?: id,
            administratorer = administratorer.map { it.navIdent },
            navRegion = navRegion?.enhetsnummer ?: "",
            navEnheter = navEnheter.map { it.enhetsnummer },
            oppstart = oppstart,
            kontaktpersoner = kontaktpersoner.map {
                TiltaksgjennomforingKontaktpersonDbo(
                    navIdent = it.navIdent,
                    navEnheter = it.navEnheter,
                    beskrivelse = it.beskrivelse,
                )
            },
            stedForGjennomforing = stedForGjennomforing,
            faneinnhold = faneinnhold,
            beskrivelse = beskrivelse,
            deltidsprosent = deltidsprosent,
            estimertVentetidVerdi = estimertVentetid?.verdi,
            estimertVentetidEnhet = estimertVentetid?.enhet,
            tilgjengeligForArrangorFraOgMedDato = tilgjengeligForArrangorFraOgMedDato,
            amoKategorisering = amoKategorisering,
            utdanningslop = utdanningslop?.toDbo(),
        )
}
