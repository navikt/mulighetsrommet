package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.routes.v1.EstimertVentetid
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
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
        apentForInnsok = true,
        antallPlasser = null,
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        avtaleId = AvtaleFixtures.oppfolging.id,
        deltidsprosent = 100.0,
    )

    val Oppfolging1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#1",
        arrangorOrganisasjonsnummer = "976663934",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(),
        navRegion = "2990",
        navEnheter = listOf("2990"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.oppfolging.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
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
        arrangorKontaktpersoner = emptyList(),
        stengtFra = Oppfolging1.stengtFra,
        stengtTil = Oppfolging1.stengtTil,
        stedForGjennomforing = Oppfolging1.stedForGjennomforing,
        avtaleId = Oppfolging1.avtaleId,
        apentForInnsok = true,
        opphav = Oppfolging1.opphav,
        faneinnhold = Oppfolging1.faneinnhold,
        beskrivelse = Oppfolging1.beskrivelse,
        deltidsprosent = 100.0,
        estimertVentetid = EstimertVentetid(
            verdi = 3,
            enhet = "dag",
        ),
    )

    val Oppfolging2 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 2",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#2",
        arrangorOrganisasjonsnummer = "111111111",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = emptyList(),
        navRegion = "2990",
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.oppfolging.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
    )

    val Arbeidstrening1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening 1",
        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
        tiltaksnummer = "2023#3",
        arrangorOrganisasjonsnummer = "222222222",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = emptyList(),
        navRegion = "2990",
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.oppfolging.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
    )
}
