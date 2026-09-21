package no.nav.mulighetsrommet.ereg

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ereg.testFixture.EregFixtures
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate

class EregClientTest : FunSpec({

    context("getHovedenhet") {
        test("skal returnere juridisk enhet direkte når orgnr er en juridisk enhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight EregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "TENOR TESTFIRMA AS",
                postadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1"),
                ),
                forretningsadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Testveien 1"),
                ),
                overordnetEnhet = null,
            )
        }

        test("skal returnere organisasjonsledd direkte uten videre oppslag") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/111111111") {
                        respondJson(EregFixtures.ORGANISASJONSLEDD)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("111111111")) shouldBeRight EregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("111111111"),
                organisasjonsform = "ORGL",
                navn = "TENOR TESTETAT",
                postadresse = null,
                forretningsadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Testveien 2"),
                ),
                overordnetEnhet = null,
            )
        }

        test("skal returnere slettet juridisk enhet når enheten har opphoersdato") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456780") {
                        respondJson(EregFixtures.JURIDISK_ENHET_SLETTET)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("123456780")) shouldBeRight SlettetEregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456780"),
                organisasjonsform = "AS",
                navn = "TENOR TESTFIRMA UNDER AVVIKLING AS",
                slettetDato = LocalDate.of(2024, 11, 26),
            )
        }

        test("skal slå opp juridisk enhet når orgnr er en virksomhet/underenhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654321") {
                        respondJson(EregFixtures.VIRKSOMHET)
                    }
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("987654321")) shouldBeRight EregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "TENOR TESTFIRMA AS",
                postadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1"),
                ),
                forretningsadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Testveien 1"),
                ),
                overordnetEnhet = null,
            )
        }

        test("skal returnere feil når virksomhet mangler knytning til juridisk enhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654321") {
                        respondJson(EregFixtures.VIRKSOMHET_UTEN_JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("987654321")) shouldBeLeft EregError.Error(
                "Fant ikke juridisk enhet for virksomhet med orgnr 987654321 i Ereg",
            )
        }

        test("skal slå opp juridisk enhet gjennom nøstet organisasjonsledd-kjede") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654323") {
                        respondJson(EregFixtures.VIRKSOMHET_MED_ORGANISASJONSLEDD)
                    }
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("987654323")) shouldBeRight EregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "TENOR TESTFIRMA AS",
                postadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1"),
                ),
                forretningsadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Testveien 1"),
                ),
                overordnetEnhet = null,
            )
        }

        test("skal returnere NotFound når orgnr ikke finnes i Ereg") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/999999999") {
                        respondError(HttpStatusCode.NotFound)
                    }
                },
            )

            eregClient.getHovedenhet(Organisasjonsnummer("999999999")) shouldBeLeft EregError.NotFound
        }
    }

    context("getUnderenhet") {
        test("skal returnere virksomhet direkte gitt orgnr") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654321") {
                        respondJson(EregFixtures.VIRKSOMHET)
                    }
                },
            )

            eregClient.getUnderenhet(Organisasjonsnummer("987654321")) shouldBeRight EregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                organisasjonsform = "BEDR",
                navn = "TENOR TESTFIRMA AS AVD OSLO",
                overordnetEnhet = Organisasjonsnummer("123456789"),
            )
        }

        test("skal returnere slettet virksomhet når enheten har opphoersdato") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654322") {
                        respondJson(EregFixtures.VIRKSOMHET_SLETTET)
                    }
                },
            )

            eregClient.getUnderenhet(Organisasjonsnummer("987654322")) shouldBeRight SlettetEregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("987654322"),
                organisasjonsform = "BEDR",
                navn = "TENOR TESTFIRMA AS AVD NEDLAGT",
                slettetDato = LocalDate.of(2023, 12, 30),
            )
        }

        test("skal returnere NotFound når orgnr ikke er en virksomhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getUnderenhet(Organisasjonsnummer("123456789")) shouldBeLeft EregError.NotFound
        }
    }

    context("getEnhet") {
        test("skal returnere juridisk enhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getEnhet(Organisasjonsnummer("123456789")) shouldBeRight EregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "TENOR TESTFIRMA AS",
                postadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Postboks 1"),
                ),
                forretningsadresse = EregAdresse(
                    landkode = "NO",
                    postnummer = "0170",
                    poststed = "OSLO",
                    adresse = listOf("Testveien 1"),
                ),
                overordnetEnhet = null,
            )
        }

        test("skal returnere virksomhet uten å slå opp juridisk enhet") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/987654321") {
                        respondJson(EregFixtures.VIRKSOMHET)
                    }
                },
            )

            eregClient.getEnhet(Organisasjonsnummer("987654321")) shouldBeRight EregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                organisasjonsform = "BEDR",
                navn = "TENOR TESTFIRMA AS AVD OSLO",
                overordnetEnhet = Organisasjonsnummer("123456789"),
            )
        }
    }

    context("getUnderenheterForHovedenhet") {
        test("skal returnere virksomhetene til den juridiske enheten") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.getUnderenheterForHovedenhet(Organisasjonsnummer("123456789")) shouldBeRight listOf(
                EregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("987654321"),
                    organisasjonsform = null,
                    navn = "TENOR TESTFIRMA AS AVD OSLO",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                ),
            )
        }
    }

    context("searchHovedenhet") {
        test("skal returnere juridiske enheter/organisasjonsledd fra søk på navn") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/finn?organisasjonsnavn=Tenor&antall=20") {
                        respondJson(EregFixtures.FINN_ORGANISASJON)
                    }
                },
            )

            eregClient.searchHovedenhet("Tenor") shouldBeRight listOf(
                EregHovedenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456789"),
                    organisasjonsform = "AS",
                    navn = "TENOR TESTFIRMA AS",
                    postadresse = null,
                    forretningsadresse = EregAdresse(
                        landkode = "NO",
                        postnummer = "0170",
                        poststed = "OSLO",
                        adresse = listOf("Testveien 1"),
                    ),
                    overordnetEnhet = null,
                ),
            )
        }

        test("skal returnere tom liste hvis søket ikke gir treff") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/finn?organisasjonsnavn=foobarbaz&antall=20") {
                        respondJson(EregFixtures.FINN_ORGANISASJON_INGEN_TREFF)
                    }
                },
            )

            eregClient.searchHovedenhet("foobarbaz") shouldBeRight emptyList()
        }

        test("skal falle tilbake til getHovedenhet når søket er et orgnr") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/123456789") {
                        respondJson(EregFixtures.JURIDISK_ENHET)
                    }
                },
            )

            eregClient.searchHovedenhet("123456789") shouldBeRight listOf(
                EregHovedenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("123456789"),
                    organisasjonsform = "AS",
                    navn = "TENOR TESTFIRMA AS",
                    postadresse = EregAdresse(
                        landkode = "NO",
                        postnummer = "0170",
                        poststed = "OSLO",
                        adresse = listOf("Postboks 1"),
                    ),
                    forretningsadresse = EregAdresse(
                        landkode = "NO",
                        postnummer = "0170",
                        poststed = "OSLO",
                        adresse = listOf("Testveien 1"),
                    ),
                    overordnetEnhet = null,
                ),
            )
        }
    }

    context("searchUnderenhet") {
        test("skal returnere virksomheter fra søk på navn") {
            val eregClient = EregClient(
                baseUrl = "https://ereg-services",
                clientEngine = createMockEngine {
                    get("/v2/organisasjon/finn?organisasjonsnavn=Tenor&antall=20") {
                        respondJson(EregFixtures.FINN_ORGANISASJON)
                    }
                },
            )

            eregClient.searchUnderenhet("Tenor") shouldBeRight listOf(
                EregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("987654321"),
                    organisasjonsform = "BEDR",
                    navn = "TENOR TESTFIRMA AS AVD OSLO",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                ),
                EregUnderenhetDto(
                    organisasjonsnummer = Organisasjonsnummer("987654322"),
                    organisasjonsform = "AAFY",
                    navn = "TENOR TESTFORENING AVD OSLO",
                    overordnetEnhet = Organisasjonsnummer("123456789"),
                ),
            )
        }
    }
})
