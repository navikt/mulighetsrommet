package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.*

class BrukerServiceTest : FunSpec({
    val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
    val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
    val veilarbpersonClient: VeilarbpersonClient = mockk()

    val brukerService = BrukerService(veilarboppfolgingClient, veilarbvedtaksstotteClient, veilarbpersonClient)
    val FNR = "12345678910"
    val FNR_2 = "99887766554"

    beforeSpec {
        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(FNR, any()) } returns Oppfolgingsstatus(
            mockOppfolgingsenhet()
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(FNR, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(FNR, any()) } returns VedtakDTO(
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        )

        coEvery { veilarbpersonClient.hentPersonInfo(FNR, any()) } returns PersonDTO(
            fornavn = "Ola"
        )

        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(FNR_2, any()) } returns Oppfolgingsstatus(
            mockOppfolgingsenhet()
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(FNR_2, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(FNR_2, any()) } returns VedtakDTO(
            innsatsgruppe = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
        )

        coEvery { veilarbpersonClient.hentPersonInfo(FNR_2, any()) } returns PersonDTO(
            fornavn = "Petter"
        )
    }

    test("Henter brukerdata for et gitt fnr") {
        brukerService.hentBrukerdata(FNR, "").fornavn shouldBe "Ola"
        brukerService.hentBrukerdata(FNR, "").innsatsgruppe shouldBe Innsatsgruppe.STANDARD_INNSATS
        brukerService.hentBrukerdata(FNR, "").fnr shouldBe FNR
        brukerService.hentBrukerdata(FNR, "").manuellStatus?.erUnderManuellOppfolging shouldBe false
        brukerService.hentBrukerdata(FNR, "").manuellStatus?.krrStatus?.erReservert shouldBe false
        brukerService.hentBrukerdata(FNR, "").manuellStatus?.krrStatus?.kanVarsles shouldBe true
        brukerService.hentBrukerdata(FNR, "").oppfolgingsenhet?.navn shouldBe "NAV Fredrikstad"
        brukerService.hentBrukerdata(FNR, "").oppfolgingsenhet?.enhetId shouldBe "0116"
    }

    test("Henting av brukerdata blir cachet basert på fnr") {
        brukerService.hentBrukerdata(FNR, "")
        brukerService.hentBrukerdata(FNR, "")
        brukerService.hentBrukerdata(FNR, "")
        brukerService.hentBrukerdata(FNR_2, "")

        coVerify(exactly = 2) { veilarboppfolgingClient.hentManuellStatus(any(), any()) }
        coVerify(exactly = 2) { veilarboppfolgingClient.hentOppfolgingsstatus(any(), any()) }
        coVerify(exactly = 2) { veilarbpersonClient.hentPersonInfo(any(), any()) }
        coVerify(exactly = 2) { veilarbvedtaksstotteClient.hentSiste14AVedtak(any(), any()) }
    }
})

fun mockManuellStatus(): ManuellStatusDTO {
    return ManuellStatusDTO(
        erUnderManuellOppfolging = false,
        krrStatus = KrrStatus(
            kanVarsles = true,
            erReservert = false
        )
    )
}

fun mockOppfolgingsenhet(): Oppfolgingsenhet {
    return Oppfolgingsenhet(navn = "NAV Fredrikstad", enhetId = "0116")
}
