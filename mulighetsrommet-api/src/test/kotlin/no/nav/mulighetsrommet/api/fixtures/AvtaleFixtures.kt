package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val oppfolging = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = "Alt er dyrt",
        administratorer = emptyList(),
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
        antallPlasser = null,
        url = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    val avtaleForVta = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for VTA",
        avtalenummer = "2024#1",
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = emptyList(),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = null,
        administratorer = emptyList(),
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
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
        administratorer = listOf("DD1"),
        url = "google.com",
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
        prisbetingelser = null,
        beskrivelse = null,
        faneinnhold = null,
    )
}
