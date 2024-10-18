package no.nav.mulighetsrommet.api.clients.brreg

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class BrregClientTest : FunSpec({

    val hovedenhet = BrregEnhet(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        navn = "NAV Hovedenhet",
        beliggenhetsadresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )
    val underenhet = BrregEnhet(
        organisasjonsnummer = Organisasjonsnummer("123456780"),
        navn = "NAV Underenhet",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        beliggenhetsadresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )

    context("sokOverordnetEnhet") {
        test("Søk etter hovedenhet skal returnere en liste med treff") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/enheter?navn=Nav" to {
                        respondJson(BrregEmbeddedHovedenheter(BrregHovedenheter(listOf(hovedenhet))))
                    },
                ),
            )

            brregClient.sokOverordnetEnhet("Nav") shouldBeRight listOf(
                BrregVirksomhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456789"),
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
                    "/enheter?navn=NAV" to {
                        respondJson(BrregEmbeddedHovedenheter())
                    },
                ),
            )

            brregClient.sokOverordnetEnhet("NAV") shouldBeRight emptyList()
        }
    }

    context("hentUnderenheterForOverordnetEnhet") {
        test("Søk etter underenheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(BrregEmbeddedUnderenheter(BrregUnderenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getUnderenheterForOverordnetEnhet(Organisasjonsnummer("123456789")) shouldBeRight listOf(
                BrregVirksomhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456780"),
                    navn = "NAV Underenhet",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                    postnummer = "1234",
                    poststed = "Oslo",
                ),
            )
        }

        test("Søk etter underenhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(BrregEmbeddedHovedenheter())
                    },
                ),
            )

            brregClient.getUnderenheterForOverordnetEnhet(Organisasjonsnummer("123456789")) shouldBeRight emptyList()
        }
    }

    context("getHovedenhet") {
        test("hent hovedenhet gitt orgnr") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(hovedenhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(BrregEmbeddedUnderenheter())
                    },
                ),
            )

            brregClient.getHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight BrregVirksomhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
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
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(BrregEmbeddedUnderenheter(BrregUnderenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight BrregVirksomhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "NAV Hovedenhet",
                underenheter = listOf(
                    BrregVirksomhetDto(
                        organisasjonsnummer = Organisasjonsnummer("123456780"),
                        navn = "NAV Underenhet",
                        overordnetEnhet = Organisasjonsnummer("123456789"),
                        postnummer = "1234",
                        poststed = "Oslo",
                    ),
                ),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }
    }

    context("getUnderenhet") {
        test("hent underenhet gitt orgnr") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/underenheter/123456780" to { respondJson(underenhet) },
                ),
            )

            brregClient.getUnderenhet(Organisasjonsnummer("123456780")) shouldBeRight BrregVirksomhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                navn = "NAV Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }
    }

    context("getBrregVirksomhet") {
        test("skal hente hovedenhet med underenheter fra brreg gitt orgnr til hovedenhet") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(hovedenhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(BrregEmbeddedUnderenheter(BrregUnderenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getBrregVirksomhet(Organisasjonsnummer("123456789")) shouldBeRight BrregVirksomhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "NAV Hovedenhet",
                underenheter = listOf(
                    BrregVirksomhetDto(
                        organisasjonsnummer = Organisasjonsnummer("123456780"),
                        navn = "NAV Underenhet",
                        overordnetEnhet = Organisasjonsnummer("123456789"),
                        postnummer = "1234",
                        poststed = "Oslo",
                    ),
                ),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }

        test("skal hente underenhet fra brreg gitt orgnr til underenhet") {
            val brregClient = BrregClient(
                "https://brreg.no",
                createMockEngine(
                    "/enheter/123456780" to {
                        respondError(HttpStatusCode.NotFound)
                    },
                    "/underenheter/123456780" to {
                        respondJson(underenhet)
                    },
                ),
            )

            brregClient.getBrregVirksomhet(Organisasjonsnummer("123456780")) shouldBeRight BrregVirksomhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                navn = "NAV Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }
    }
})
