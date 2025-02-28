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

    context("sokOverordnetEnhet") {
        test("Søk etter enheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter?navn=Digitaliseringsdirektoratet") {
                        respondJson(BrregFixtures.SOK_ENHET)
                    }
                },
            )

            brregClient.sokHovedenhet("Digitaliseringsdirektoratet") shouldBeRight listOf(
                BrregHovedenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("991825827"),
                    organisasjonsform = "ORGL",
                    navn = "DIGITALISERINGSDIREKTORATET",
                    postadresse = BrregAdresse(
                        landkode = "NO",
                        postnummer = "0114",
                        poststed = "OSLO",
                        adresse = listOf("Postboks 1382 Vika"),
                    ),
                ),
            )
        }

        test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter?navn=foobarbaz") {
                        respondJson(BrregFixtures.SOK_ENHET_INGEN_TREFF)
                    }
                },
            )

            brregClient.sokHovedenhet("foobarbaz") shouldBeRight emptyList()
        }
    }

    context("hentUnderenheterForOverordnetEnhet") {
        test("Søk etter underenheter skal returnere en liste med treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter?overordnetEnhet=991825827") {
                        respondJson(BrregFixtures.SOK_UNDERENHET)
                    }
                },
            )

            brregClient.getUnderenheterForHovedenhet(Organisasjonsnummer("991825827")) shouldBeRight listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("924203617"),
                    organisasjonsform = "BEDR",
                    navn = "DIGITALISERINGSDIREKTORATET AVD BRØNNØYSUND",
                    overordnetEnhet = Organisasjonsnummer("991825827"),
                ),
            )
        }

        test("Søk etter underenhet skal returnere en tom liste hvis ingen treff") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter?overordnetEnhet=123456789") {
                        respondJson(BrregFixtures.SOK_UNDERENHET_INGEN_TREFF)
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
                    get("/enhetsregisteret/api/enheter/991825827") {
                        respondJson(BrregFixtures.ENHET)
                    }
                },
            )

            brregClient.getHovedenhet(Organisasjonsnummer("991825827")) shouldBeRight BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("991825827"),
                organisasjonsform = "ORGL",
                navn = "DIGITALISERINGSDIREKTORATET",
                postadresse = BrregAdresse(
                    landkode = "NO",
                    postnummer = "0114",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1382 Vika"),
                ),
            )
        }

        test("hent slettet enhet gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/821227062") {
                        respondJson(BrregFixtures.ENHET_SLETTET)
                    }
                },
            )

            brregClient.getHovedenhet(Organisasjonsnummer("821227062")) shouldBeRight SlettetBrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("821227062"),
                organisasjonsform = "FYLK",
                navn = "VESTFOLD OG TELEMARK FYLKESKOMMUNE UNDER SLETTING FRA 01.01.2024",
                slettetDato = LocalDate.of(2024, 11, 26),
            )
        }
    }

    context("getUnderenhet") {
        test("hent underenhet gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter/924203617") {
                        respondJson(BrregFixtures.UNDERENHET)
                    }
                },
            )

            brregClient.getUnderenhet(Organisasjonsnummer("924203617")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("924203617"),
                organisasjonsform = "BEDR",
                navn = "DIGITALISERINGSDIREKTORATET AVD BRØNNØYSUND",
                overordnetEnhet = Organisasjonsnummer("991825827"),
            )
        }

        test("hent slettet underenhet gitt orgnr") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/underenheter/974567989") {
                        respondJson(BrregFixtures.UNDERENHET_SLETTET)
                    }
                },
            )

            brregClient.getUnderenhet(Organisasjonsnummer("974567989")) shouldBeRight SlettetBrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("974567989"),
                organisasjonsform = "BEDR",
                navn = "VESTFOLD OG TELEMARK FYLKESKOMMUNE AVD SKIEN OPPLÆRING, KULTUR OG TANNHELSE",
                slettetDato = LocalDate.of(2023, 12, 30),
            )
        }
    }

    context("getBrregEnhet") {
        test("skal hente hovedenhet fra brreg gitt orgnr til hovedenhet") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/991825827") {
                        respondJson(BrregFixtures.ENHET)
                    }
                },
            )

            brregClient.getBrregEnhet(Organisasjonsnummer("991825827")) shouldBeRight BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("991825827"),
                organisasjonsform = "ORGL",
                navn = "DIGITALISERINGSDIREKTORATET",
                postadresse = BrregAdresse(
                    landkode = "NO",
                    postnummer = "0114",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1382 Vika"),
                ),
            )
        }

        test("skal hente underenhet når det ikke finnes blandt enheter fra brreg gitt orgnr til underenhet") {
            val brregClient = BrregClient(
                clientEngine = createMockEngine {
                    get("/enhetsregisteret/api/enheter/924203617") {
                        respondError(HttpStatusCode.NotFound)
                    }
                    get("/enhetsregisteret/api/underenheter/924203617") {
                        respondJson(BrregFixtures.UNDERENHET)
                    }
                },
            )

            brregClient.getBrregEnhet(Organisasjonsnummer("924203617")) shouldBeRight BrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("924203617"),
                organisasjonsform = "BEDR",
                navn = "DIGITALISERINGSDIREKTORATET AVD BRØNNØYSUND",
                overordnetEnhet = Organisasjonsnummer("991825827"),
            )
        }
    }
})
