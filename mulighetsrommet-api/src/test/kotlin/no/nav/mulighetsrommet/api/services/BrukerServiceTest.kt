package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.oppfolging.*
import no.nav.mulighetsrommet.api.clients.person.PersonDto
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
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
        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(FNR, any()) } returns OppfolgingsstatusDto(
            oppfolgingsenhet = mockOppfolgingsenhet(),
            servicegruppe = "IKKE_VURDERT",
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(FNR, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(FNR, any()) } returns VedtakDto(
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
        )

        coEvery { veilarbpersonClient.hentPersonInfo(FNR, any()) } returns PersonDto(
            fornavn = "Ola",
        )

        coEvery { veilarboppfolgingClient.hentOppfolgingsstatus(FNR_2, any()) } returns OppfolgingsstatusDto(
            oppfolgingsenhet = mockOppfolgingsenhet(),
            servicegruppe = "IKKE_VURDERT",
        )

        coEvery { veilarboppfolgingClient.hentManuellStatus(FNR_2, any()) } returns mockManuellStatus()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(FNR_2, any()) } returns VedtakDto(
            innsatsgruppe = Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
        )

        coEvery { veilarbpersonClient.hentPersonInfo(FNR_2, any()) } returns PersonDto(
            fornavn = "Petter",
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
    return Oppfolgingsenhet(navn = "NAV Fredrikstad", enhetId = "0116")
}
