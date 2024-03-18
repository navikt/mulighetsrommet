package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val oppfolging = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        url = "https://www.websak.no",
        beskrivelse = null,
        faneinnhold = null,
    )

    val oppfolgingMedAvtale = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        url = "https://www.websak.no",
        beskrivelse = null,
        faneinnhold = null,
    )

    val gruppeAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo",
        avtalenummer = "2024#8",
        tiltakstypeId = TiltakstypeFixtures.GRUPPE_AMO.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OffentligOffentlig,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        url = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    val VTA = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for VTA",
        avtalenummer = "2024#1",
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Forhaandsgodkjent,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        url = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    val AFT = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for AFT",
        avtalenummer = "2024#1",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        avtaletype = Avtaletype.Forhaandsgodkjent,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        url = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        administratorer = listOf(NavIdent("DD1")),
        url = "google.com",
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
        prisbetingelser = null,
        beskrivelse = null,
        faneinnhold = null,
    )
}
