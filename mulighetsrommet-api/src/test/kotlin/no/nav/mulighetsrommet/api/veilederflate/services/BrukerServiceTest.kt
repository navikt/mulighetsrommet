package no.nav.mulighetsrommet.api.veilederflate.services

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
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.IsoppfolgingstilfelleClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.KrrStatus
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.Oppfolgingsenhet
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.vedtak.Gjeldende14aVedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.HovedmalMedOkeDeltakelse
import no.nav.mulighetsrommet.api.clients.vedtak.InnsatsgruppeV2
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentBrukerPdlQuery
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentBrukerResponse
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.ZonedDateTime

class BrukerServiceTest : FunSpec({
    val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
    val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
    val navEnhetService: NavEnhetService = mockk()
    val brukerQuery: HentBrukerPdlQuery = mockk()
    val norg2Client: Norg2Client = mockk()
    val isoppfolgingstilfelleClient: IsoppfolgingstilfelleClient = mockk()

    val brukerService = BrukerService(
        veilarboppfolgingClient,
        veilarbvedtaksstotteClient,
        navEnhetService,
        norg2Client,
        isoppfolgingstilfelleClient,
        brukerQuery,
    )
    val fnr1 = NorskIdent("12345678910")
    val fnr2 = NorskIdent("99887766554")

    val navEgneAnsatteEnhet = NavEnhetDto(
        navn = "Nav egne ansatte Lerkendal",
        enhetsnummer = NavEnhetNummer("1683"),
        type = NavEnhetType.KO,
        overordnetEnhet = NavEnhetNummer("0500"),
    )

    val navLerkendalEnhet = NavEnhetDto(
        navn = "Nav Lerkendal",
        enhetsnummer = NavEnhetNummer("0501"),
        type = NavEnhetType.LOKAL,
        overordnetEnhet = NavEnhetNummer("0500"),
    )

    val navVikafossenEnhet = NavEnhetDto(
        navn = "Nav Vikafossen",
        enhetsnummer = NavEnhetNummer("2103"),
        type = NavEnhetType.KO,
        overordnetEnhet = NavEnhetNummer("2100"),
    )

    beforeSpec {
        coEvery { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr1, any()) } returns true.right()
        coEvery { veilarboppfolgingClient.hentOppfolgingsenhet(fnr1, any()) } returns mockOppfolgingsenhet().right()

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr1, any()) } returns mockManuellStatus().right()
        coEvery { isoppfolgingstilfelleClient.erSykmeldtMedArbeidsgiver(fnr1) } returns true.right()

        coEvery { veilarbvedtaksstotteClient.hentGjeldende14aVedtak(fnr1, any()) } returns Gjeldende14aVedtakDto(
            innsatsgruppe = InnsatsgruppeV2.GODE_MULIGHETER,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now(),
        ).right()

        coEvery { brukerQuery.hentBruker(PdlIdent(fnr1.value), any()) } answers {
            HentBrukerResponse("Ola", GeografiskTilknytning.GtKommune("0301")).right()
        }

        coEvery { brukerQuery.hentBruker(PdlIdent(fnr2.value), any()) } answers {
            HentBrukerResponse("Petter", GeografiskTilknytning.GtKommune("0301")).right()
        }

        coEvery { norg2Client.hentEnhetByGeografiskOmraade(any()) } returns Norg2EnhetDto(
            enhetId = 1,
            navn = "Nav Fredrikstad",
            enhetNr = NavEnhetNummer("0106"),
            status = Norg2EnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
        ).right()

        coEvery { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr2, any()) } returns true.right()
        coEvery { veilarboppfolgingClient.hentOppfolgingsenhet(fnr2, any()) } returns mockOppfolgingsenhet().right()

        coEvery { veilarboppfolgingClient.hentManuellStatus(fnr2, any()) } returns mockManuellStatus().right()

        coEvery { veilarbvedtaksstotteClient.hentGjeldende14aVedtak(fnr2, any()) } returns Gjeldende14aVedtakDto(
            innsatsgruppe = InnsatsgruppeV2.JOBBE_DELVIS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now(),
        ).right()

        coEvery { navEnhetService.hentEnhet(NavEnhetNummer("0106")) } returns NavEnhetDto(
            navn = "Nav Fredrikstad",
            enhetsnummer = NavEnhetNummer("0106"),
            type = NavEnhetType.LOKAL,
            overordnetEnhet = NavEnhetNummer("0100"),
        )
    }

    test("Henter brukerdata for et gitt fnr") {
        brukerService.hentBrukerdata(fnr1, AccessType.OBO("")) shouldBe
            Brukerdata(
                fornavn = "Ola",
                innsatsgruppe = Innsatsgruppe.GODE_MULIGHETER,
                fnr = fnr1,
                manuellStatus = ManuellStatusDto(
                    erUnderManuellOppfolging = false,
                    krrStatus = KrrStatus(
                        erReservert = false,
                        kanVarsles = true,
                    ),
                ),
                enheter = listOf(
                    NavEnhetDto(
                        navn = "Nav Fredrikstad",
                        enhetsnummer = NavEnhetNummer("0106"),
                        type = NavEnhetType.LOKAL,
                        overordnetEnhet = NavEnhetNummer("0100"),
                    ),
                ),
                erUnderOppfolging = true,
                erSykmeldtMedArbeidsgiver = true,
                varsler = emptyList(),
            )
    }

    test("Exception kastes hvis personinfo mangler") {
        coEvery { brukerQuery.hentBruker(PdlIdent(fnr1.value), any()) } returns PdlError.Error.left()

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
            val oppfolgingsenhet =
                navEgneAnsatteEnhet.copy(enhetsnummer = NavEnhetNummer("0502"), type = NavEnhetType.LOKAL)
            getRelevanteEnheterForBruker(navLerkendalEnhet, oppfolgingsenhet).should {
                it shouldContainExactly listOf(oppfolgingsenhet)
            }
        }

        test("Hent relevante enheter returnerer tom liste hvis oppfølgingsenhet ikke er blant egne ansatte") {
            getRelevanteEnheterForBruker(null, navVikafossenEnhet).should {
                it shouldBe emptyList()
            }
        }
    }

    context("Varsler ang. bruker til veileder") {
        test("Skal returnere true når oppfolgingsenhet er lokal enhet og oppfølgingsenhet er ulik geografisk enhet") {
            val result = erOppfolgingsenhetEnAnnenGeografiskEnhet(
                geografiskEnhet = NavEnhetDto(
                    navn = "",
                    enhetsnummer = NavEnhetNummer("1234"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("1000"),
                ),
                oppfolgingsenhet = NavEnhetDto(
                    navn = "",
                    enhetsnummer = NavEnhetNummer("4321"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("4000"),
                ),
            )
            result shouldBe true
        }
        test("Skal returnere false når oppfolgingsenhet er lokal enhet og oppfølgingsenhet er lik geografisk enhet") {
            val result = erOppfolgingsenhetEnAnnenGeografiskEnhet(
                geografiskEnhet = NavEnhetDto(
                    navn = "",
                    enhetsnummer = NavEnhetNummer("1234"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("1000"),
                ),
                oppfolgingsenhet = NavEnhetDto(
                    navn = "",
                    enhetsnummer = NavEnhetNummer("1234"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("1000"),
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
    return Oppfolgingsenhet(navn = "Nav Fredrikstad", enhetId = NavEnhetNummer("0106"))
}
