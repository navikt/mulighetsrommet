package no.nav.mulighetsrommet.brreg

import org.intellij.lang.annotations.Language

object BrregFixtures {
    @Language("JSON")
    val ENHET = """
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
    """.trimIndent()

    @Language("JSON")
    val ENHET_SLETTET = """
        {
          "organisasjonsnummer": "821227062",
          "navn": "VESTFOLD OG TELEMARK FYLKESKOMMUNE UNDER SLETTING FRA 01.01.2024",
          "organisasjonsform": {
            "kode": "FYLK",
            "beskrivelse": "Fylkeskommune",
            "_links": {
              "self": {
                "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/FYLK"
              }
            }
          },
          "slettedato": "2024-11-26",
          "_links": {
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter/821227062"
            }
          }
        }
    """.trimIndent()

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
    val SOK_ENHET_INGEN_TREFF = """
        {
          "_links": {
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/enheter?navn=foobarbaz*&size=1"
            }
          },
          "page": {
            "size": 1,
            "totalElements": 0,
            "totalPages": 0,
            "number": 0
          }
        }
    """.trimIndent()

    @Language("JSON")
    val UNDERENHET = """
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
    """.trimIndent()

    @Language("JSON")
    val UNDERENHET_SLETTET = """
        {
          "organisasjonsnummer": "974567989",
          "navn": "VESTFOLD OG TELEMARK FYLKESKOMMUNE AVD SKIEN OPPLÆRING, KULTUR OG TANNHELSE",
          "organisasjonsform": {
            "kode": "BEDR",
            "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning",
            "_links": {
              "self": {
                "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/BEDR"
              }
            }
          },
          "slettedato": "2023-12-30",
          "nedleggelsesdato": "2023-12-30",
          "_links": {
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/974567989"
            }
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

    @Language("JSON")
    val SOK_UNDERENHET_INGEN_TREFF = """
        {
          "_links": {
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/underenheter?overordnetEnhet=123456789&size=1"
            }
          },
          "page": {
            "size": 1,
            "totalElements": 0,
            "totalPages": 0,
            "number": 0
          }
        }
    """.trimIndent()
}
