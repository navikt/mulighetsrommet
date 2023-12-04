package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
import java.util.*

object TiltaksgjennomforingFixtures {
    val ArenaOppfolging1 = ArenaTiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#1",
        arrangorOrganisasjonsnummer = "976663934",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        avtaleId = AvtaleFixtures.avtale1.id,
        fremmoteDato = null,
        fremmoteSted = null,
    )

    val Oppfolging1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#1",
        arrangorOrganisasjonsnummer = "976663934",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = 12,
        administratorer = listOf(),
        navRegion = "2990",
        navEnheter = listOf("2990"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersonId = null,
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.avtale1.id,
        faneinnhold = null,
        beskrivelse = null,
    )

    val Oppfolging1Request = TiltaksgjennomforingRequest(
        id = Oppfolging1.id,
        navn = Oppfolging1.navn,
        tiltakstypeId = Oppfolging1.tiltakstypeId,
        tiltaksnummer = Oppfolging1.tiltaksnummer,
        arrangorOrganisasjonsnummer = Oppfolging1.arrangorOrganisasjonsnummer,
        startDato = Oppfolging1.startDato,
        sluttDato = Oppfolging1.sluttDato,
        antallPlasser = Oppfolging1.antallPlasser,
        administratorer = listOf("DD1"),
        navRegion = "2990",
        navEnheter = listOf("2990"),
        oppstart = Oppfolging1.oppstart,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersonId = Oppfolging1.arrangorKontaktpersonId,
        stengtFra = Oppfolging1.stengtFra,
        stengtTil = Oppfolging1.stengtTil,
        stedForGjennomforing = Oppfolging1.stedForGjennomforing,
        avtaleId = Oppfolging1.avtaleId,
        apenForInnsok = true,
        opphav = Oppfolging1.opphav,
        faneinnhold = Oppfolging1.faneinnhold,
        beskrivelse = Oppfolging1.beskrivelse,
    )

    val Oppfolging1AdminDto = TiltaksgjennomforingAdminDto(
        id = Oppfolging1.id,
        tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
            id = TiltakstypeFixtures.Oppfolging.id,
            navn = TiltakstypeFixtures.Oppfolging.navn,
            arenaKode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        navn = Oppfolging1.navn,
        tiltaksnummer = Oppfolging1.tiltaksnummer,
        arrangor = TiltaksgjennomforingAdminDto.Arrangor(
            organisasjonsnummer = Oppfolging1.arrangorOrganisasjonsnummer,
            navn = "Bedrift",
            kontaktperson = null,
            slettet = false,
        ),
        startDato = Oppfolging1.startDato,
        sluttDato = Oppfolging1.sluttDato,
        arenaAnsvarligEnhet = null,
        status = Tiltaksgjennomforingsstatus.GJENNOMFORES,
        tilgjengelighet = Oppfolging1.tilgjengelighet,
        antallPlasser = Oppfolging1.antallPlasser,
        avtaleId = Oppfolging1.avtaleId,
        administratorer = emptyList(),
        navEnheter = emptyList(),
        navRegion = null,
        sanityId = null,
        oppstart = Oppfolging1.oppstart,
        opphav = Oppfolging1.opphav,
        stengtFra = Oppfolging1.stengtFra,
        stengtTil = Oppfolging1.stengtTil,
        kontaktpersoner = emptyList(),
        stedForGjennomforing = Oppfolging1.stedForGjennomforing,
        faneinnhold = Oppfolging1.faneinnhold,
        beskrivelse = Oppfolging1.beskrivelse,
        createdAt = Oppfolging1.startDato.atStartOfDay(),
        updatedAt = Oppfolging1.startDato.atStartOfDay(),
        tilgjengeligForVeileder = true,
        visesForVeileder = true,
        fremmoteDato = null,
        fremmoteSted = null,
    )

    val Oppfolging2 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 2",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#2",
        arrangorOrganisasjonsnummer = "111111111",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = 12,
        administratorer = emptyList(),
        navRegion = "2990",
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersonId = null,
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.avtale1.id,
        faneinnhold = null,
        beskrivelse = null,
    )

    val Arbeidstrening1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening 1",
        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
        tiltaksnummer = "2023#3",
        arrangorOrganisasjonsnummer = "222222222",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = 12,
        administratorer = emptyList(),
        navRegion = "2990",
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersonId = null,
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.avtale1.id,
        faneinnhold = null,
        beskrivelse = null,
    )
}
