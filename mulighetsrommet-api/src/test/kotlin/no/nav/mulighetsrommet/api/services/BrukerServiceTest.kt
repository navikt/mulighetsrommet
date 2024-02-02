package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.*
import no.nav.mulighetsrommet.api.clients.person.Enhet
import no.nav.mulighetsrommet.api.clients.person.PersonDto
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus

class BrukerServiceTest : FunSpec({
    val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
    val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
    val veilarbpersonClient: VeilarbpersonClient = mockk()
    val navEnhetService: NavEnhetService = mockk()

    val brukerService =
        BrukerService(veilarboppfolgingClient, veilarbvedtaksstotteClient, veilarbpersonClient, navEnhetService)
    val fnr1 = "12345678910"
    val fnr2 = "99887766554"

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

    beforeSpec {
        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(fnr1, any()) } returns OppfolgingsstatusDto(
            oppfolgingsenhet = mockOppfolgingsenhet(),
            servicegruppe = "IKKE_VURDERT",
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr1, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr1, any()) } returns VedtakDto(
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
        )

        coEvery { veilarbpersonClient.hentPersonInfo(fnr1, any()) } returns PersonDto(
            fornavn = "Ola",
            geografiskEnhet = Enhet(
                navn = "NAV Fredrikstad",
                enhetsnummer = "0106",
            ),
        )

        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(fnr2, any()) } returns OppfolgingsstatusDto(
            oppfolgingsenhet = mockOppfolgingsenhet(),
            servicegruppe = "IKKE_VURDERT",
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr2, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr2, any()) } returns VedtakDto(
            innsatsgruppe = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
        )

        coEvery { veilarbpersonClient.hentPersonInfo(fnr2, any()) } returns PersonDto(
            fornavn = "Petter",
            geografiskEnhet = Enhet(
                navn = "NAV Fredrikstad",
                enhetsnummer = "0106",
            ),
        )

        coEvery { navEnhetService.hentEnhet(any()) } returns NavEnhetDbo(
            navn = "NAV Fredrikstad",
            enhetsnummer = "0106",
            status = NavEnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
            overordnetEnhet = "0100",
        )
    }

    test("Henter brukerdata for et gitt fnr") {
        brukerService.hentBrukerdata(fnr1, "").fornavn shouldBe "Ola"
        brukerService.hentBrukerdata(fnr1, "").innsatsgruppe shouldBe Innsatsgruppe.STANDARD_INNSATS
        brukerService.hentBrukerdata(fnr1, "").fnr shouldBe fnr1
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.erUnderManuellOppfolging shouldBe false
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.krrStatus?.erReservert shouldBe false
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.krrStatus?.kanVarsles shouldBe true
        brukerService.hentBrukerdata(fnr1, "").enheter shouldContainExactly listOf(
            NavEnhetDbo(
                navn = "NAV Fredrikstad",
                enhetsnummer = "0106",
                type = Norg2Type.LOKAL,
                overordnetEnhet = "0100",
                status = NavEnhetStatus.AKTIV,
            ),
        )
    }

    context("getRelevanteEnheterForBruker") {
        test("Hent relevante enheter returnerer liste med både geografisk- og oppfølgingsenhet hvis oppfølgingsenhet ikke er et fylke eller lokalkontor") {
            getRelevanteEnheterForBruker(navLerkendalEnhet, navEgneAnsatteEnhet).should {
                it shouldContainInOrder listOf(navLerkendalEnhet, navEgneAnsatteEnhet)
            }
        }

        test("Hent relevante enheter returnerer liste med geografisk enhet hvis oppfølgingsenhet ikke eksisterer") {
            getRelevanteEnheterForBruker(navLerkendalEnhet, null).should {
                it shouldContainExactly listOf(navLerkendalEnhet)
            }
        }

        test("Hent relevante enheter returnerer liste med oppfølgingsenhet enhet hvis oppfølgingsenhet er Lokal") {
            val oppfolgingsenhet = navEgneAnsatteEnhet.copy(enhetsnummer = "0502", type = Norg2Type.LOKAL)
            getRelevanteEnheterForBruker(navLerkendalEnhet, oppfolgingsenhet).should {
                it shouldContainExactly listOf(oppfolgingsenhet)
            }
        }
    }

    context("Varsler ang. bruker til veileder") {
        test("Skal returnere true når oppfolgingsenhet er lokal enhet og oppfølgingsenhet er ulik geografisk enhet") {
            val result = oppfolgingsenhetLokalOgUlik(
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "1234",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "1000"
                ),
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "4321",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "4000"
                )
            )
            result shouldBe true
        }
        test("Skal returnere false når oppfolgingsenhet er lokal enhet og oppfølgingsenhet er lik geografisk enhet") {
            val result = oppfolgingsenhetLokalOgUlik(
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "1234",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "1000"
                ),
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "1234",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "1000"
                ),
            )
            result shouldBe false
        }
    }
})

fun mockManuellStatus(): ManuellStatusDto {
    return ManuellStatusDto(
        erUnderManuellOppfolging = false,
        krrStatus = KrrStatus(
            kanVarsles = true,
            erReservert = false,
        ),
    )
}

fun mockOppfolgingsenhet(): Oppfolgingsenhet {
    return Oppfolgingsenhet(navn = "NAV Fredrikstad", enhetId = "0106")
}
