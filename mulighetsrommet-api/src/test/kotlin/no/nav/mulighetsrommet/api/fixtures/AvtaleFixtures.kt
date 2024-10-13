package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.Opsjonsmodell
import no.nav.mulighetsrommet.api.routes.v1.OpsjonsmodellData
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val oppfolging = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val oppfolgingMedAvtale = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val gruppeAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo",
        avtalenummer = "2024#8",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OffentligOffentlig,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = Faneinnhold(kurstittel = "Min kurstittel"),
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val gruppeFagYrke = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo",
        avtalenummer = "2024#8",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OffentligOffentlig,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = Faneinnhold(kurstittel = "Min kurstittel"),
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val IPS = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "IPS",
        avtalenummer = "2024#9",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.IPS.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OffentligOffentlig,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val VTA = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for VTA",
        avtalenummer = "2024#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Forhaandsgodkjent,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = null,
        opsjonsmodell = null,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val AFT = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for AFT",
        avtalenummer = "2024#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        avtaletype = Avtaletype.Forhaandsgodkjent,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = null,
        opsjonsmodell = null,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val EnkelAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for EnkelAmo",
        avtalenummer = "2024#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        avtaletype = Avtaletype.Forhaandsgodkjent,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val jobbklubb = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Jobbklubb avtale",
        avtalenummer = "2023#13",
        websaknummer = Websaknummer("24/3234"),
        tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        websaknummer = Websaknummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorOrganisasjonsnummer = ArrangorFixtures.hovedenhet.organisasjonsnummer,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("2990"),
        arrangorKontaktpersoner = emptyList(),
        prisbetingelser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodellData = OpsjonsmodellData(
            opsjonMaksVarighet = LocalDate.now().plusYears(5),
            opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
            customOpsjonsmodellNavn = null,
        ),
        utdanningslop = null,
    )
}
