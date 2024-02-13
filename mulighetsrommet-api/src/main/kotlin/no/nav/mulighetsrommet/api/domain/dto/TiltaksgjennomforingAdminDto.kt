package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
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
    val arrangor: Arrangor,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: NavEnhetDbo?,
    val status: Tiltaksgjennomforingsstatus,
    val apentForInnsok: Boolean,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val administratorer: List<Administrator>,
    val navEnheter: List<NavEnhetDbo>,
    val navRegion: NavEnhetDbo?,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate?,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktperson>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val tilgjengeligForAlle: Boolean,
    val visesForAlle: Boolean,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
) {
    fun isAktiv(): Boolean = status in listOf(
        Tiltaksgjennomforingsstatus.PLANLAGT,
        Tiltaksgjennomforingsstatus.GJENNOMFORES,
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
        val navIdent: String,
        val navn: String,
    )

    @Serializable
    data class Arrangor(
        val organisasjonsnummer: String,
        val navn: String?,
        val kontaktpersoner: List<VirksomhetKontaktperson>,
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
            tiltaksnummer = tiltaksnummer,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer,
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
            opphav = opphav,
            stengtFra = stengtFra,
            stengtTil = stengtTil,
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

    fun toArenaTiltaksgjennomforingDbo() =
        ArenaTiltaksgjennomforingDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = tiltaksnummer ?: "",
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = arenaAnsvarligEnhet?.enhetsnummer,
            avslutningsstatus = when (status) {
                Tiltaksgjennomforingsstatus.PLANLAGT, Tiltaksgjennomforingsstatus.GJENNOMFORES -> Avslutningsstatus.IKKE_AVSLUTTET
                Tiltaksgjennomforingsstatus.AVLYST -> Avslutningsstatus.AVLYST
                Tiltaksgjennomforingsstatus.AVBRUTT -> Avslutningsstatus.AVBRUTT
                Tiltaksgjennomforingsstatus.AVSLUTTET -> Avslutningsstatus.AVSLUTTET
            },
            apentForInnsok = apentForInnsok,
            antallPlasser = antallPlasser,
            avtaleId = avtaleId,
            oppstart = oppstart,
            opphav = opphav,
            deltidsprosent = deltidsprosent,
        )
}
