package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltaksgjennomforingAdminDto(
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
    val status: TiltaksgjennomforingStatus,
    val apentForInnsok: Boolean,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val administratorer: List<Administrator>,
    val navRegion: NavEnhetDbo?,
    val navEnheter: List<NavEnhetDbo>,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktperson>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val publisert: Boolean,
    val publisertForAlle: Boolean,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
    val personvernBekreftet: Boolean,
) {
    fun isAktiv(): Boolean = status in listOf(
        TiltaksgjennomforingStatus.PLANLAGT,
        TiltaksgjennomforingStatus.GJENNOMFORES,
    )

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
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
        val organisasjonsnummer: String,
        val navn: String,
        val kontaktpersoner: List<ArrangorKontaktperson>,
        val slettet: Boolean,
    )

    @Serializable
    data class EstimertVentetid(
        val verdi: Int,
        val enhet: String,
    )

    fun toDbo() =
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
        )
}
