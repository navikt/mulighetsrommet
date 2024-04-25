package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.Oppfolgingsenhet
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlPerson
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.ktor.exception.StatusException

class BrukerServiceTest : FunSpec({
    val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
    val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
    val navEnhetService: NavEnhetService = mockk()
    val pdlClient: PdlClient = mockk()
    val norg2Client: Norg2Client = mockk()

    val brukerService =
        BrukerService(veilarboppfolgingClient, veilarbvedtaksstotteClient, navEnhetService, pdlClient, norg2Client)
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
        coEvery { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr1, any()) } returns true.right()
        coEvery { veilarboppfolgingClient.hentOppfolgingsenhet(fnr1, any()) } returns mockOppfolgingsenhet().right()

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr1, any()) } returns mockManuellStatus().right()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr1, any()) } returns VedtakDto(
            innsatsgruppe = VedtakDto.Innsatsgruppe.STANDARD_INNSATS,
        ).right()

        coEvery { pdlClient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytning.GtKommune(value = "0301").right()

        coEvery { pdlClient.hentPerson(fnr1, any()) } returns PdlPerson(
            navn = listOf(PdlPerson.PdlNavn(fornavn = "Ola")),
        ).right()

        coEvery { norg2Client.hentEnhetByGeografiskOmraade(any()) } returns Norg2EnhetDto(
            enhetId = 1,
            navn = "NAV Fredrikstad",
            enhetNr = "0106",
            status = Norg2EnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
        ).right()

        coEvery { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr2, any()) } returns true.right()
        coEvery { veilarboppfolgingClient.hentOppfolgingsenhet(fnr2, any()) } returns mockOppfolgingsenhet().right()

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr2, any()) } returns mockManuellStatus().right()

        coEvery { veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr2, any()) } returns VedtakDto(
            innsatsgruppe = VedtakDto.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
        ).right()

        coEvery { pdlClient.hentPerson(fnr2, any()) } returns PdlPerson(
            navn = listOf(PdlPerson.PdlNavn(fornavn = "Petter")),
        ).right()

        coEvery { navEnhetService.hentEnhet(any()) } returns NavEnhetDbo(
            navn = "NAV Fredrikstad",
            enhetsnummer = "0106",
            status = NavEnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
            overordnetEnhet = "0100",
        )
    }

    test("Henter brukerdata for et gitt fnr") {
        brukerService.hentBrukerdata(fnr1, AccessType.OBO("")) shouldBe
            BrukerService.Brukerdata(
                fornavn = "Ola",
                innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
                fnr = fnr1,
                manuellStatus = ManuellStatusDto(
                    erUnderManuellOppfolging = false,
                    krrStatus = ManuellStatusDto.KrrStatus(
                        erReservert = false,
                        kanVarsles = true,
                    ),
                ),
                enheter = listOf(
                    NavEnhetDbo(
                        navn = "NAV Fredrikstad",
                        enhetsnummer = "0106",
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = "0100",
                        status = NavEnhetStatus.AKTIV,
                    ),
                ),
                erUnderOppfolging = true,
                varsler = emptyList(),
            )
    }

    test("Exception kastes ved tom enhetsliste") {
        coEvery { pdlClient.hentGeografiskTilknytning(fnr1, any()) } returns PdlError.NotFound.left()
        coEvery { veilarboppfolgingClient.hentOppfolgingsenhet(fnr1, any()) } returns OppfolgingError.NotFound.left()

        shouldThrow<StatusException> {
            brukerService.hentBrukerdata(fnr1, AccessType.OBO(""))
        }
    }

    test("Exception kastes hvis personinfo mangler") {
        coEvery { pdlClient.hentPerson(fnr1, any()) } returns PdlError.Error.left()

        shouldThrow<StatusException> {
            brukerService.hentBrukerdata(fnr1, AccessType.OBO(""))
        }
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
                    overordnetEnhet = "1000",
                ),
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "4321",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "4000",
                ),
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
                    overordnetEnhet = "1000",
                ),
                NavEnhetDbo(
                    navn = "",
                    enhetsnummer = "1234",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "1000",
                ),
            )
            result shouldBe false
        }
    }
})

fun mockManuellStatus(): ManuellStatusDto {
    return ManuellStatusDto(
        erUnderManuellOppfolging = false,
        krrStatus = ManuellStatusDto.KrrStatus(
            kanVarsles = true,
            erReservert = false,
        ),
    )
}

fun mockOppfolgingsenhet(): Oppfolgingsenhet {
    return Oppfolgingsenhet(navn = "NAV Fredrikstad", enhetId = "0106")
}
