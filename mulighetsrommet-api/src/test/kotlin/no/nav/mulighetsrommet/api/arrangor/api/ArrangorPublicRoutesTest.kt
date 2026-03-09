package no.nav.mulighetsrommet.api.arrangor.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language

class ArrangorPublicRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    )

    beforeSpec {
        oauth.start()

        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf()),
    )

    context("/v1/arrangor") {
        context("sok hovedenhet") {
            val sokUrl = { term: String -> "/api/v1/arrangor/hovedenhet/sok/$term" }

            test("401 når påkrevde claims mangler fra token") {
                withTestApplication(appConfig()) {
                    val term = "Tiger"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles()).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            test("200 når søk returneres") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/enheter") {
                            respondJson(SOK_ENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val term = "Tiger"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregHovedenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }
        }

        context("hent underenheter") {
            val underenhetUrl = { orgnr: Organisasjonsnummer -> "/api/v1/arrangor/hovedenhet/$orgnr/underenheter" }

            test("401 når påkrevde claims mangler fra token") {
                withTestApplication(appConfig()) {
                    val orgnr = Organisasjonsnummer("123456789")
                    val response = client.get(underenhetUrl(orgnr)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles()).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            test("200 når søk returneres") {
                val orgnr = Organisasjonsnummer("924203617")

                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(SOK_UNDERENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val response = client.get(underenhetUrl(orgnr)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }
        }
    }
})

private fun withApplicationRoles(roles: String? = null): Map<String, List<String>> = mapOf(
    "roles" to listOfNotNull(AppRoles.ACCESS_AS_APPLICATION, roles),
)

@Language("JSON")
val SOK_ENHET = """
        {
          "_embedded": {
            "enheter": [
              {
                "organisasjonsnummer": "991825827",
                "navn": "DIGITALISERINGSDIREKTORATET",
                "organisasjonsform": {
                  "kode": "ORGL",
                  "beskrivelse": "Organisasjonsledd",
                  "_links": {
                    "self": {
                      "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/ORGL"
                    }
                  }
                },
                "hjemmeside": "www.digdir.no",
                "postadresse": {
                  "land": "Norge",
                  "landkode": "NO",
                  "postnummer": "0114",
                  "poststed": "OSLO",
                  "adresse": [
                    "Postboks 1382 Vika"
                  ],
                  "kommune": "OSLO",
                  "kommunenummer": "0301"
                },
                "registreringsdatoEnhetsregisteret": "2007-10-15",
                "registrertIMvaregisteret": false,
                "naeringskode1": {
                  "kode": "84.110",
                  "beskrivelse": "Generell offentlig administrasjon"
                },
                "antallAnsatte": 433,
                "harRegistrertAntallAnsatte": true,
                "overordnetEnhet": "932384469",
                "registreringsdatoAntallAnsatteEnhetsregisteret": "2025-02-12",
                "registreringsdatoAntallAnsatteNAVAaregisteret": "2025-02-10",
                "epostadresse": "postmottak@digdir.no",
                "telefon": "22 45 10 00",
                "forretningsadresse": {
                  "land": "Norge",
                  "landkode": "NO",
                  "postnummer": "0585",
                  "poststed": "OSLO",
                  "adresse": [
                    "Lørenfaret 1C"
                  ],
                  "kommune": "OSLO",
                  "kommunenummer": "0301"
                },
                "institusjonellSektorkode": {
                  "kode": "6100",
                  "beskrivelse": "Statsforvaltningen"
                },
                "registrertIForetaksregisteret": false,
                "registrertIStiftelsesregisteret": false,
                "registrertIFrivillighetsregisteret": false,
                "konkurs": false,
                "underAvvikling": false,
                "underTvangsavviklingEllerTvangsopplosning": false,
                "maalform": "Bokmål",
                "aktivitet": [
                  "Digitaliseringsdirektoratet skal være regjeringens fremste verktøy for",
                  "raskere og mer samordnet digitalisering av offentlig sektor, og bidra",
                  "til formålstjenlig digitalisering av samfunnet som helhet."
                ],
                "registrertIPartiregisteret": false,
                "_links": {
                  "self": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/enheter/991825827"
                  },
                  "overordnetEnhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/enheter/932384469"
                  }
                }
              }
            ]
          },
          "_links": {
            "first": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter?navn=Digitaliseringsdirektoratet*&page=0&size=1"
            },
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter?navn=Digitaliseringsdirektoratet*&size=1"
            },
            "next": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter?navn=Digitaliseringsdirektoratet*&page=1&size=1"
            },
            "last": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter?navn=Digitaliseringsdirektoratet*&page=2&size=1"
            }
          },
          "page": {
            "size": 1,
            "totalElements": 3,
            "totalPages": 3,
            "number": 0
          }
        }
""".trimIndent()

@Language("JSON")
val SOK_UNDERENHET = """
        {
          "_embedded": {
            "underenheter": [
              {
                "organisasjonsnummer": "924203617",
                "navn": "DIGITALISERINGSDIREKTORATET AVD BRØNNØYSUND",
                "organisasjonsform": {
                  "kode": "BEDR",
                  "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning",
                  "_links": {
                    "self": {
                      "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/BEDR"
                    }
                  }
                },
                "hjemmeside": "www.digdir.no",
                "registreringsdatoEnhetsregisteret": "2019-12-16",
                "registrertIMvaregisteret": false,
                "naeringskode1": {
                  "kode": "84.110",
                  "beskrivelse": "Generell offentlig administrasjon"
                },
                "antallAnsatte": 94,
                "harRegistrertAntallAnsatte": true,
                "overordnetEnhet": "991825827",
                "registreringsdatoAntallAnsatteEnhetsregisteret": "2025-02-11",
                "registreringsdatoAntallAnsatteNAVAaregisteret": "2025-02-10",
                "epostadresse": "postmottak@digdir.no",
                "oppstartsdato": "2020-01-01",
                "beliggenhetsadresse": {
                  "land": "Norge",
                  "landkode": "NO",
                  "postnummer": "8900",
                  "poststed": "BRØNNØYSUND",
                  "adresse": [
                    "Havnegata 48"
                  ],
                  "kommune": "BRØNNØY",
                  "kommunenummer": "1813"
                },
                "_links": {
                  "self": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/924203617"
                  },
                  "overordnetEnhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/enheter/991825827"
                  }
                }
              }
            ]
          },
          "_links": {
            "first": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter?overordnetEnhet=991825827&page=0&size=1"
            },
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter?overordnetEnhet=991825827&size=1"
            },
            "next": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter?overordnetEnhet=991825827&page=1&size=1"
            },
            "last": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter?overordnetEnhet=991825827&page=2&size=1"
            }
          },
          "page": {
            "size": 1,
            "totalElements": 3,
            "totalPages": 3,
            "number": 0
          }
        }
""".trimIndent()
