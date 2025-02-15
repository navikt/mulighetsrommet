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
        postadresse = Adresse(landkode = "NO", poststed = "Oslo", postnummer = "1234", adresse = listOf("Gateveien 1")),
    )
    val underenhet = Underenhet(
        organisasjonsnummer = Organisasjonsnummer("123456780"),
        organisasjonsform = Organisasjonsform(
            kode = "BEDR",
            beskrivelse = "Underenhet til næringsdrivende og offentlig forvaltning",
        ),
        navn = "Nav Underenhet",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        beliggenhetsadresse = null,
    )

    context("sokOverordnetEnhet") {
        test("Søk etter enheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter?navn=Nav") {
                        respondJson(EmbeddedEnheter(EmbeddedEnheter.Enheter(listOf(enhet))))
                    }
                },
            )

            brregClient.sokHovedenhet("Nav") shouldBeRight listOf(
                BrregHovedenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456789"),
                    organisasjonsform = "AS",
                    navn = "Nav Hovedenhet",
                    postadresse = BrregAdresse(
                        landkode = "NO",
                        postnummer = "1234",
                        poststed = "Oslo",
                        adresse = listOf("Gateveien 1"),
                    ),
                ),
            )
        }

        test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter?navn=Nav") {
                        respondJson(EmbeddedEnheter())
                    }
                },
            )

            brregClient.sokHovedenhet("Nav") shouldBeRight emptyList()
        }
    }

    context("hentUnderenheterForOverordnetEnhet") {
        test("Søk etter underenheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter?overordnetEnhet=123456789") {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    }
                },
            )

            brregClient.getUnderenheterForHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456780"),
                    organisasjonsform = "BEDR",
                    navn = "Nav Underenhet",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                ),
            )
        }

        test("Søk etter underenhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter?overordnetEnhet=123456789") {
                        respondJson(EmbeddedEnheter())
                    }
                },
            )

            brregClient.getUnderenheterForHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight emptyList()
        }
    }

    context("getEnhet") {
        test("hent hovedenhet uten underenheter gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/123456789") {
                        respondJson(enhet)
                    }
                    get("/enhetsregisteret/api/underenheter?overordnetEnhet=123456789") {
                        respondJson(EmbeddedUnderenheter(EmbeddedUnderenheter.Underenheter(listOf(underenhet))))
                    }
                },
            )

            brregClient.getHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                postadresse = BrregAdresse(
                    landkode = "NO",
                    postnummer = "1234",
                    poststed = "Oslo",
                    adresse = listOf("Gateveien 1"),
                ),
            )
        }

        test("hent slettet enhet gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/974291657") {
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
                    }
                },
            )

            brregClient.getHovedenhet(Organisasjonsnummer("974291657")) shouldBeRight SlettetBrregHovedenhetDto(
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
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter/123456780") {
                        respondJson(underenhet)
                    }
                },
            )

            brregClient.getUnderenhet(Organisasjonsnummer("123456780")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                organisasjonsform = "BEDR",
                navn = "Nav Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
            )
        }

        test("hent slettet underenhet gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter/974291657") {
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
                    }
                },
            )

            brregClient.getUnderenhet(Organisasjonsnummer("974291657")) shouldBeRight SlettetBrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("974291657"),
                organisasjonsform = "BEDR",
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
            )
        }
    }

    context("getBrregEnhet") {
        test("skal hente hovedenhet fra brreg gitt orgnr til hovedenhet") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/123456789") {
                        respondJson(enhet)
                    }
                },
            )

            brregClient.getBrregEnhet(Organisasjonsnummer("123456789")) shouldBeRight BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Nav Hovedenhet",
                postadresse = BrregAdresse(
                    landkode = "NO",
                    postnummer = "1234",
                    poststed = "Oslo",
                    adresse = listOf("Gateveien 1"),
                ),
            )
        }

        test("skal hente underenhet når det ikke finnes blandt enheter fra brreg gitt orgnr til underenhet") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/123456780") {
                        respondError(HttpStatusCode.NotFound)
                    }
                    get("/enhetsregisteret/api/underenheter/123456780") {
                        respondJson(underenhet)
                    }
                },
            )

            brregClient.getBrregEnhet(Organisasjonsnummer("123456780")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                organisasjonsform = "BEDR",
                navn = "Nav Underenhet",
                overordnetEnhet = Organisasjonsnummer("123456789"),
            )
        }
    }
})
