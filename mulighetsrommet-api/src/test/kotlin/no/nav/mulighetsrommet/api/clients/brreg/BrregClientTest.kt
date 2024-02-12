package no.nav.mulighetsrommet.api.clients.brreg

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class BrregClientTest : FunSpec({

    val hovedenhet = BrregEnhet(
        organisasjonsnummer = "123456789",
        navn = "NAV Hovedenhet",
        beliggenhetsadresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )
    val underenhet = BrregEnhet(
        organisasjonsnummer = "123456780",
        navn = "NAV Underenhet",
        overordnetEnhet = "123456789",
        beliggenhetsadresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )

    test("Søk etter hovedenhet skal returnere en liste med treff") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/enheter?size=20&navn=Nav" to {
                    respondJson(BrregEmbeddedHovedenheter(BrregHovedenheter(listOf(hovedenhet))))
                },
            ),
        )

        brregClient.sokEtterOverordnetEnheter("Nav") shouldBeRight listOf(
            VirksomhetDto(
                organisasjonsnummer = "123456789",
                navn = "NAV Hovedenhet",
                postnummer = "1234",
                poststed = "Oslo",
            ),
        )
    }

    test("Søk etter hovedenhet skal returnere en tom liste hvis ingen treff") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/enheter?size=20&navn=NAV" to {
                    respondJson(BrregEmbeddedHovedenheter())
                },
            ),
        )

        brregClient.sokEtterOverordnetEnheter("NAV") shouldBeRight emptyList()
    }

    test("Søk etter underenheter skal returnere en liste med treff") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/underenheter?size=1000&overordnetEnhet=123456789" to {
                    respondJson(BrregEmbeddedUnderenheter(BrregUnderenheter(listOf(underenhet))))
                },
            ),
        )

        brregClient.hentUnderenheterForOverordnetEnhet("123456789") shouldBeRight listOf(
            VirksomhetDto(
                organisasjonsnummer = "123456780",
                navn = "NAV Underenhet",
                overordnetEnhet = "123456789",
                postnummer = "1234",
                poststed = "Oslo",
            ),
        )
    }

    test("Søk etter underenhet skal returnere en tom liste hvis ingen treff") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/underenheter?size=1000&overordnetEnhet=123456789" to {
                    respondJson(BrregEmbeddedHovedenheter())
                },
            ),
        )

        brregClient.hentUnderenheterForOverordnetEnhet("123456789") shouldBeRight emptyList()
    }

    test("hent hovedenhet gitt orgnr") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/enheter/123456789" to {
                    respondJson(hovedenhet)
                },
                "/underenheter?size=1000&overordnetEnhet=123456789" to {
                    respondJson(BrregEmbeddedUnderenheter())
                },
            ),
        )

        brregClient.getHovedenhet("123456789") shouldBeRight VirksomhetDto(
            organisasjonsnummer = "123456789",
            navn = "NAV Hovedenhet",
            underenheter = emptyList(),
            postnummer = "1234",
            poststed = "Oslo",
        )
    }

    test("hent hovedenhet med underenheter gitt orgnr") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/enheter/123456789" to {
                    respondJson(hovedenhet)
                },
                "/underenheter?size=1000&overordnetEnhet=123456789" to {
                    respondJson(BrregEmbeddedUnderenheter(BrregUnderenheter(listOf(underenhet))))
                },
            ),
        )

        brregClient.getHovedenhet("123456789") shouldBeRight VirksomhetDto(
            organisasjonsnummer = "123456789",
            navn = "NAV Hovedenhet",
            underenheter = listOf(
                VirksomhetDto(
                    organisasjonsnummer = "123456780",
                    navn = "NAV Underenhet",
                    overordnetEnhet = "123456789",
                    postnummer = "1234",
                    poststed = "Oslo",
                ),
            ),
            postnummer = "1234",
            poststed = "Oslo",
        )
    }

    test("hent underenhet gitt orgnr") {
        val brregClient = BrregClient(
            "https://brreg.no",
            createMockEngine(
                "/underenheter/123456780" to { respondJson(underenhet) },
            ),
        )

        brregClient.getUnderenhet("123456780") shouldBeRight VirksomhetDto(
            organisasjonsnummer = "123456780",
            navn = "NAV Underenhet",
            overordnetEnhet = "123456789",
            postnummer = "1234",
            poststed = "Oslo",
        )
    }
})
