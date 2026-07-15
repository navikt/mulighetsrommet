package no.nav.mulighetsrommet.api.enhetsregister

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.Virksomhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.FjernetBrregEnhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate

class BrregEnhetsregisterGatewayTest : FunSpec({
    test("sokHovedenheter mapper brreg-hovedenheter til Hovedenhet") {
        val brregClient: BrregClient = mockk {
            coEvery { searchHovedenhet("nord") } returns listOf(
                BrregHovedenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("111111111"),
                    organisasjonsform = "AS",
                    navn = "Nord AS",
                    postadresse = null,
                    forretningsadresse = null,
                    overordnetEnhet = null,
                ),
            ).right()
        }
        val gateway = BrregEnhetsregisterGateway(brregClient)

        val hovedenheter = gateway.sokHovedenheter("nord").shouldBeRight()

        hovedenheter shouldBe listOf(
            Virksomhet.Hovedenhet(
                organisasjonsnummer = Organisasjonsnummer("111111111"),
                navn = "Nord AS",
                organisasjonsform = "AS",
                overordnetEnhet = null,
            ),
        )
    }

    test("sokUnderenheter mapper brreg-underenheter til Underenhet") {
        val brregClient: BrregClient = mockk {
            coEvery { searchUnderenhet("nord") } returns listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("333333333"),
                    organisasjonsform = "BEDR",
                    navn = "Nord Avdeling",
                    overordnetEnhet = Organisasjonsnummer("111111111"),
                ),
            ).right()
        }
        val gateway = BrregEnhetsregisterGateway(brregClient)

        val underenheter = gateway.sokUnderenheter("nord").shouldBeRight()

        underenheter shouldBe listOf(
            Virksomhet.Underenhet(
                organisasjonsnummer = Organisasjonsnummer("333333333"),
                navn = "Nord Avdeling",
                organisasjonsform = "BEDR",
                overordnetEnhet = Organisasjonsnummer("111111111"),
            ),
        )
    }

    test("hentUnderenheterForHovedenhet mapper brreg-underenheter, inkludert slettede") {
        val hovedenhetOrgnr = Organisasjonsnummer("111111111")

        val brregClient: BrregClient = mockk {
            coEvery { getUnderenheterForHovedenhet(hovedenhetOrgnr) } returns listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("333333333"),
                    organisasjonsform = "BEDR",
                    navn = "Nord Avdeling",
                    overordnetEnhet = hovedenhetOrgnr,
                ),
                SlettetBrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("444444444"),
                    organisasjonsform = "BEDR",
                    navn = "Nord Avdeling Slettet",
                    slettetDato = LocalDate.of(2020, 1, 1),
                ),
            ).right()
        }
        val gateway = BrregEnhetsregisterGateway(brregClient)

        val underenheter = gateway.hentUnderenheterForHovedenhet(hovedenhetOrgnr).shouldBeRight()

        underenheter shouldBe listOf(
            Virksomhet.Underenhet(
                organisasjonsnummer = Organisasjonsnummer("333333333"),
                navn = "Nord Avdeling",
                organisasjonsform = "BEDR",
                overordnetEnhet = hovedenhetOrgnr,
            ),
            Virksomhet.Underenhet(
                organisasjonsnummer = Organisasjonsnummer("444444444"),
                navn = "Nord Avdeling Slettet",
                organisasjonsform = "BEDR",
                overordnetEnhet = hovedenhetOrgnr,
                slettetDato = LocalDate.of(2020, 1, 1),
            ),
        )
    }

    context("hentVirksomhet") {
        test("hovedenhet fra brreg mappes til Funnet") {
            val orgnr = Organisasjonsnummer("999999999")
            val brregClient = mockk<BrregClient> {
                coEvery { getBrregEnhet(orgnr) } returns BrregHovedenhetDto(
                    organisasjonsnummer = orgnr,
                    organisasjonsform = "AS",
                    navn = "Fretex AS",
                    postadresse = null,
                    forretningsadresse = null,
                    overordnetEnhet = null,
                ).right()
            }
            val enhetsregister = BrregEnhetsregisterGateway(brregClient)

            enhetsregister.hentVirksomhet(orgnr).shouldBeRight().shouldBeTypeOf<VirksomhetOppslag.Funnet>() should {
                it.virksomhet.organisasjonsnummer shouldBe orgnr
                it.virksomhet.navn shouldBe "Fretex AS"
            }
        }

        test("underenhet fra brreg mappes til Funnet") {
            val orgnr = Organisasjonsnummer("999999998")
            val brregClient = mockk<BrregClient> {
                coEvery { getBrregEnhet(orgnr) } returns BrregUnderenhetDto(
                    organisasjonsnummer = orgnr,
                    organisasjonsform = "BEDR",
                    navn = "Fretex AS avd Oslo",
                    overordnetEnhet = Organisasjonsnummer("999999999"),
                ).right()
            }
            val enhetsregister = BrregEnhetsregisterGateway(brregClient)

            enhetsregister.hentVirksomhet(orgnr).shouldBeRight().shouldBeTypeOf<VirksomhetOppslag.Funnet>() should {
                it.virksomhet.organisasjonsnummer shouldBe orgnr
                it.virksomhet.navn shouldBe "Fretex AS avd Oslo"
            }
        }

        test("virksomhet fjernet av juridiske årsaker mappes til FjernetAvJuridiskeArsaker") {
            val orgnr = Organisasjonsnummer("999999997")
            val slettetDato = LocalDate.of(2020, 1, 1)
            val brregClient = mockk<BrregClient> {
                coEvery { getBrregEnhet(orgnr) } returns BrregError.FjernetAvJuridiskeArsaker(
                    FjernetBrregEnhetDto(orgnr, slettetDato),
                ).left()
            }
            val enhetsregister = BrregEnhetsregisterGateway(brregClient)

            enhetsregister.hentVirksomhet(orgnr).shouldBeRight(
                VirksomhetOppslag.FjernetAvJuridiskeArsaker(orgnr, slettetDato),
            )
        }

        test("NotFound fra brreg mappes til EnhetsregisterError.IkkeFunnet") {
            val orgnr = Organisasjonsnummer("999999996")
            val brregClient = mockk<BrregClient> {
                coEvery { getBrregEnhet(orgnr) } returns BrregError.NotFound.left()
            }
            val enhetsregister = BrregEnhetsregisterGateway(brregClient)

            enhetsregister.hentVirksomhet(orgnr).shouldBeLeft(EnhetsregisterError.IkkeFunnet)
        }

        test("andre feil fra brreg mappes til EnhetsregisterError.Feil") {
            val orgnr = Organisasjonsnummer("999999995")
            val brregClient = mockk<BrregClient> {
                coEvery { getBrregEnhet(orgnr) } returns BrregError.Error.left()
            }
            val enhetsregister = BrregEnhetsregisterGateway(brregClient)

            enhetsregister.hentVirksomhet(orgnr).shouldBeLeft(EnhetsregisterError.Feil)
        }
    }
})
