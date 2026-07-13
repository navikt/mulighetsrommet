package no.nav.mulighetsrommet.api.enhetsregister

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
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
            Hovedenhet(
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
            Underenhet(
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
            Underenhet(
                organisasjonsnummer = Organisasjonsnummer("333333333"),
                navn = "Nord Avdeling",
                organisasjonsform = "BEDR",
                overordnetEnhet = hovedenhetOrgnr,
            ),
            Underenhet(
                organisasjonsnummer = Organisasjonsnummer("444444444"),
                navn = "Nord Avdeling Slettet",
                organisasjonsform = "BEDR",
                overordnetEnhet = hovedenhetOrgnr,
                slettetDato = LocalDate.of(2020, 1, 1),
            ),
        )
    }
})
