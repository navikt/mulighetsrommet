package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.gjennomforing.EstimertVentetid
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

object TiltaksgjennomforingFixtures {
    val Oppfolging1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = AvtaleFixtures.oppfolging.startDato.plusDays(1),
        sluttDato = AvtaleFixtures.oppfolging.startDato.plusMonths(3),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.oppfolging.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val EnkelAmo1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "EnkelAmo 1",
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = AvtaleFixtures.oppfolging.startDato.plusDays(1),
        sluttDato = AvtaleFixtures.oppfolging.startDato.plusMonths(3),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.EnkelAmo.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val Oppfolging1Request = TiltaksgjennomforingRequest(
        id = Oppfolging1.id,
        navn = Oppfolging1.navn,
        tiltakstypeId = Oppfolging1.tiltakstypeId,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = Oppfolging1.startDato,
        sluttDato = Oppfolging1.sluttDato,
        antallPlasser = Oppfolging1.antallPlasser,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "2990",
        navEnheter = listOf("2990"),
        oppstart = Oppfolging1.oppstart,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = Oppfolging1.stedForGjennomforing,
        avtaleId = Oppfolging1.avtaleId,
        apentForInnsok = true,
        faneinnhold = Oppfolging1.faneinnhold,
        beskrivelse = Oppfolging1.beskrivelse,
        deltidsprosent = 100.0,
        tilgjengeligForArrangorFraOgMedDato = null,
        estimertVentetid = EstimertVentetid(
            verdi = 3,
            enhet = "dag",
        ),
        amoKategorisering = null,
        utdanningslop = null,
    )

    val Oppfolging2 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging 2",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorId = ArrangorFixtures.underenhet2.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = emptyList(),
        navRegion = "2990",
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.oppfolging.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val VTA1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "VTA 1",
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        apentForInnsok = true,
        antallPlasser = 12,
        navRegion = "0400",
        navEnheter = listOf("0502"),
        administratorer = listOf(NavIdent("DD1")),
        oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.VTA.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val AFT1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "AFT 1",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.AFT.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val Jobbklubb1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Jobbklubb 1",
        tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.jobbklubb.id,
        faneinnhold = Faneinnhold(kurstittel = "Jobbklubb 1"),
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val GruppeAmo1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo 1",
        tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.gruppeAmo.id,
        faneinnhold = Faneinnhold(kurstittel = "Gruppe amo 1"),
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val GruppeFagYrke1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Fag- og yrkesopplæring 1",
        tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.gruppeFagYrke.id,
        faneinnhold = Faneinnhold(kurstittel = "Gruppe fag og yrke 1"),
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val IPS1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "IPS 1",
        tiltakstypeId = TiltakstypeFixtures.IPS.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.IPS.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    val ArbeidsrettetRehabilitering = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidsretter Rehabilitering 1",
        tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        apentForInnsok = true,
        antallPlasser = 12,
        administratorer = listOf(NavIdent("DD1")),
        navRegion = "0400",
        navEnheter = listOf("0502"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
        stedForGjennomforing = "Oslo",
        avtaleId = AvtaleFixtures.ArbeidsrettetRehabilitering.id,
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorFraOgMedDato = null,
        amoKategorisering = null,
        utdanningslop = null,
    )
}
