package no.nav.mulighetsrommet.brreg

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate

class BrregClientTest : FunSpec({

    val enhet = OverordnetEnhet(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = Organisasjonsform(kode = "AS", beskrivelse = "Aksjeselskap"),
        navn = "Nav Hovedenhet",
        postAdresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )
    val underenhet = Underenhet(
        organisasjonsnummer = Organisasjonsnummer("123456780"),
        organisasjonsform = Organisasjonsform(
            kode = "BEDR",
            beskrivelse = "Underenhet til næringsdrivende og offentlig forvaltning",
        ),
        navn = "Nav Underenhet",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        beliggenhetsadresse = Adresse(poststed = "Oslo", postnummer = "1234"),
    )

    context("sokOverordnetEnhet") {
        test("Søk etter enheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter?navn=Nav" to {
                        respondJson(EmbeddedEnheter(EmbeddedEnheter.Enheter(listOf(enhet))))
                    },
                ),
            )

            brregClient.sokOverordnetEnhet("Nav") shouldBeRight listOf(
                BrregEnhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456789"),
                    organisasjonsform = "AS",
                    navn = "Nav Hovedenhet",
                    postnummer = "1234",
                    poststed = "Oslo",
                ),
            )
        }

        test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter?navn=Nav" to {
                        respondJson(EmbeddedEnheter())
                    },
                ),
            )

            brregClient.sokOverordnetEnhet("Nav") shouldBeRight emptyList()
        }
    }

    context("hentUnderenheterForOverordnetEnhet") {
        test("Søk etter underenheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getUnderenheterForOverordnetEnhet(Organisasjonsnummer("123456789")) shouldBeRight listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456780"),
                    organisasjonsform = "BEDR",
                    navn = "Nav Underenhet",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                    postnummer = "1234",
                    poststed = "Oslo",
                ),
            )
        }

        test("Søk etter underenhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedEnheter())
                    },
                ),
            )

            brregClient.getUnderenheterForOverordnetEnhet(Organisasjonsnummer("123456789")) shouldBeRight emptyList()
        }
    }

    context("getEnhetMedUnderenheter") {
        test("hent hovedenhet gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(enhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedUnderenheter())
                    },
                ),
            )

            brregClient.getEnhetMedUnderenheter(Organisasjonsnummer("123456789")) shouldBeRight BrregEnhetMedUnderenheterDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                underenheter = emptyList(),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }

        test("hent hovedenhet med underenheter gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(enhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getEnhetMedUnderenheter(Organisasjonsnummer("123456789")) shouldBeRight BrregEnhetMedUnderenheterDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                underenheter = listOf(
                    BrregUnderenhetDto(
                        organisasjonsnummer = Organisasjonsnummer("123456780"),
                        organisasjonsform = "BEDR",
                        navn = "Nav Underenhet",
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

    context("getEnhet") {
        test("hent hovedenhet uten underenheter gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(enhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getEnhet(Organisasjonsnummer("123456789")) shouldBeRight BrregEnhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                postnummer = "1234",
                poststed = "Oslo",
            )
        }

        test("hent slettet enhet gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/974291657" to {
                        respondJson(
                            OverordnetEnhet(
                                organisasjonsnummer = Organisasjonsnummer("974291657"),
                                organisasjonsform = Organisasjonsform(
                                    kode = "AS",
                                    beskrivelse = "Aksjeselskap",
                                ),
                                navn = "Slettet bedrift",
                                slettedato = LocalDate.of(2020, 1, 1),
                            ),
                        )
                    },
                ),
            )

            brregClient.getEnhet(Organisasjonsnummer("974291657")) shouldBeRight SlettetBrregEnhetDto(
                organisasjonsnummer = Organisasjonsnummer("974291657"),
                organisasjonsform = "AS",
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
            )
        }
    }

    context("getUnderenhet") {
        test("hent underenhet gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/underenheter/123456780" to { respondJson(underenhet) },
                ),
            )

            brregClient.getUnderenhet(Organisasjonsnummer("123456780")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                organisasjonsform = "BEDR",
                navn = "Nav Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }

        test("hent slettet underenhet gitt orgnr") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/underenheter/974291657" to {
                        respondJson(
                            Underenhet(
                                organisasjonsnummer = Organisasjonsnummer("974291657"),
                                organisasjonsform = Organisasjonsform(
                                    kode = "BEDR",
                                    beskrivelse = "Underenhet til næringsdrivende og offentlig forvaltning",
                                ),
                                navn = "Slettet bedrift",
                                slettedato = LocalDate.of(2020, 1, 1),
                            ),
                        )
                    },
                ),
            )

            brregClient.getUnderenhet(Organisasjonsnummer("974291657")) shouldBeRight SlettetBrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("974291657"),
                organisasjonsform = "BEDR",
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
            )
        }
    }

    context("getBrregVirksomhet") {
        test("skal hente hovedenhet med underenheter fra brreg gitt orgnr til hovedenhet") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/123456789" to {
                        respondJson(enhet)
                    },
                    "/underenheter?overordnetEnhet=123456789" to {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    },
                ),
            )

            brregClient.getBrregVirksomhet(Organisasjonsnummer("123456789")) shouldBeRight BrregEnhetMedUnderenheterDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                underenheter = listOf(
                    BrregUnderenhetDto(
                        organisasjonsnummer = Organisasjonsnummer("123456780"),
                        organisasjonsform = "BEDR",
                        navn = "Nav Underenhet",
                        overordnetEnhet = Organisasjonsnummer("123456789"),
                        postnummer = "1234",
                        poststed = "Oslo",
                    ),
                ),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }

        test("skal hente underenhet når det ikke finnes blandt enheter fra brreg gitt orgnr til underenhet") {
            val brregClient = BrregClient(
                baseUrl = "https://brreg.no",
                clientEngine = createMockEngine(
                    "/enheter/123456780" to {
                        respondError(HttpStatusCode.NotFound)
                    },
                    "/underenheter/123456780" to {
                        respondJson(underenhet)
                    },
                ),
            )

            brregClient.getBrregVirksomhet(Organisasjonsnummer("123456780")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                organisasjonsform = "BEDR",
                navn = "Nav Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                postnummer = "1234",
                poststed = "Oslo",
            )
        }
    }
})
