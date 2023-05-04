package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.oppfolging.*
import no.nav.mulighetsrommet.api.clients.person.Enhet
import no.nav.mulighetsrommet.api.clients.person.PersonDto
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient

class BrukerServiceTest : FunSpec({
    val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
    val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
    val veilarbpersonClient: VeilarbpersonClient = mockk()

    val brukerService = BrukerService(veilarboppfolgingClient, veilarbvedtaksstotteClient, veilarbpersonClient)
    val fnr1 = "12345678910"
    val fnr2 = "99887766554"

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
    }

    test("Henter brukerdata for et gitt fnr") {
        brukerService.hentBrukerdata(fnr1, "").fornavn shouldBe "Ola"
        brukerService.hentBrukerdata(fnr1, "").innsatsgruppe shouldBe Innsatsgruppe.STANDARD_INNSATS
        brukerService.hentBrukerdata(fnr1, "").fnr shouldBe fnr1
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.erUnderManuellOppfolging shouldBe false
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.krrStatus?.erReservert shouldBe false
        brukerService.hentBrukerdata(fnr1, "").manuellStatus?.krrStatus?.kanVarsles shouldBe true
        brukerService.hentBrukerdata(fnr1, "").oppfolgingsenhet?.navn shouldBe "NAV Fredrikstad"
        brukerService.hentBrukerdata(fnr1, "").oppfolgingsenhet?.enhetId shouldBe "0106"
        brukerService.hentBrukerdata(fnr1, "").geografiskEnhet?.navn shouldBe "NAV Fredrikstad"
        brukerService.hentBrukerdata(fnr1, "").geografiskEnhet?.enhetsnummer shouldBe "0106"
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
