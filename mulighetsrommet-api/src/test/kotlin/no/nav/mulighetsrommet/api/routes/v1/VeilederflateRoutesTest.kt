package no.nav.mulighetsrommet.api.routes.v1

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.Oppfolgingsenhet
import no.nav.mulighetsrommet.api.clients.person.Enhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.NavEnhetService

class VeilederflateRoutesTest : FunSpec({
    val navEnhetService: NavEnhetService = mockk(relaxed = true)

    val navEgneAnsatteEnhet = NavEnhetDbo(
        navn = "Nav egne ansatte Lerkendal",
        enhetsnummer = "0583",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.KO,
        overordnetEnhet = "0500",
    )

    val navLerkendalEnhet = NavEnhetDbo(
        navn = "Nav Lerkendal",
        enhetsnummer = "0501",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = "0500",
    )

    test("Hent relevante enheter returnerer liste med både geografisk- og oppfølgingsenhet hvis oppfølgingsenhet ikke er et fylke eller lokalkontor") {
        coEvery { navEnhetService.hentEnhet("0501") } returns navLerkendalEnhet
        coEvery { navEnhetService.hentEnhet("0583") } returns navEgneAnsatteEnhet

        val brukerdata: BrukerService.Brukerdata = BrukerService.Brukerdata(
            fnr = "12345678910",
            innsatsgruppe = null,
            oppfolgingsenhet = Oppfolgingsenhet(
                navn = navEgneAnsatteEnhet.navn,
                enhetId = navEgneAnsatteEnhet.enhetsnummer,
            ),
            geografiskEnhet = Enhet(navn = navLerkendalEnhet.navn, enhetsnummer = navLerkendalEnhet.enhetsnummer),
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )

        val result = getRelevanteEnheterForBruker(brukerdata, navEnhetService)
        result shouldContainInOrder listOf("0501", "0583")
    }

    test("Hent relevante enheter returnerer liste med geografisk enhet hvis oppfølgingsenhet ikke eksisterer") {
        coEvery { navEnhetService.hentEnhet("0501") } returns navLerkendalEnhet
        coEvery { navEnhetService.hentEnhet("0502") } returns navEgneAnsatteEnhet.copy(enhetsnummer = "0502", type = Norg2Type.LOKAL)

        val brukerdata: BrukerService.Brukerdata = BrukerService.Brukerdata(
            fnr = "12345678910",
            innsatsgruppe = null,
            oppfolgingsenhet = null,
            geografiskEnhet = Enhet(navn = navLerkendalEnhet.navn, enhetsnummer = navLerkendalEnhet.enhetsnummer),
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )

        val result = getRelevanteEnheterForBruker(brukerdata, navEnhetService)
        result shouldContainInOrder listOf("0501")
    }

    test("Hent relevante enheter returnerer liste med oppfølgingsenhet enhet hvis oppfølgingsenhet er Lokal") {
        coEvery { navEnhetService.hentEnhet("0501") } returns navLerkendalEnhet
        coEvery { navEnhetService.hentEnhet("0502") } returns navEgneAnsatteEnhet.copy(enhetsnummer = "0502", type = Norg2Type.LOKAL)

        val brukerdata: BrukerService.Brukerdata = BrukerService.Brukerdata(
            fnr = "12345678910",
            innsatsgruppe = null,
            oppfolgingsenhet = Oppfolgingsenhet(
                navn = navEgneAnsatteEnhet.navn,
                enhetId = "0502",
            ),
            geografiskEnhet = Enhet(navn = navLerkendalEnhet.navn, enhetsnummer = navLerkendalEnhet.enhetsnummer),
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )

        val result = getRelevanteEnheterForBruker(brukerdata, navEnhetService)
        result shouldContainInOrder listOf("0502")
    }
})
