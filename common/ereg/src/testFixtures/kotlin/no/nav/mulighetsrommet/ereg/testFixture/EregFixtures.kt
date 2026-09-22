package no.nav.mulighetsrommet.ereg.testFixture

import org.intellij.lang.annotations.Language

object EregFixtures {
    @Language("JSON")
    val JURIDISK_ENHET = """
        {
          "organisasjonsnummer": "123456789",
          "navn": {
            "sammensattnavn": "TENOR TESTFIRMA AS",
            "navnelinje1": "TENOR TESTFIRMA AS"
          },
          "type": "JuridiskEnhet",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "AS"
              }
            ],
            "postadresser": [
              {
                "adresselinje1": "Postboks 1",
                "postnummer": "0170",
                "poststed": "OSLO",
                "landkode": "NO"
              }
            ],
            "forretningsadresser": [
              {
                "adresselinje1": "Testveien 1",
                "postnummer": "0170",
                "poststed": "OSLO",
                "landkode": "NO"
              }
            ]
          },
          "driverVirksomheter": [
            {
              "organisasjonsnummer": "987654321",
              "navn": {
                "sammensattnavn": "TENOR TESTFIRMA AS AVD OSLO",
                "navnelinje1": "TENOR TESTFIRMA AS AVD OSLO"
              }
            }
          ]
        }
    """.trimIndent()

    @Language("JSON")
    val JURIDISK_ENHET_SLETTET = """
        {
          "organisasjonsnummer": "123456780",
          "navn": {
            "sammensattnavn": "TENOR TESTFIRMA UNDER AVVIKLING AS",
            "navnelinje1": "TENOR TESTFIRMA UNDER AVVIKLING AS"
          },
          "type": "JuridiskEnhet",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "AS"
              }
            ],
            "opphoersdato": "2024-11-26"
          }
        }
    """.trimIndent()

    @Language("JSON")
    val ORGANISASJONSLEDD = """
        {
          "organisasjonsnummer": "111111111",
          "navn": {
            "sammensattnavn": "TENOR TESTETAT",
            "navnelinje1": "TENOR TESTETAT"
          },
          "type": "Organisasjonsledd",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "ORGL"
              }
            ],
            "forretningsadresser": [
              {
                "adresselinje1": "Testveien 2",
                "postnummer": "0170",
                "poststed": "OSLO",
                "landkode": "NO"
              }
            ]
          }
        }
    """.trimIndent()

    @Language("JSON")
    val VIRKSOMHET = """
        {
          "organisasjonsnummer": "987654321",
          "navn": {
            "sammensattnavn": "TENOR TESTFIRMA AS AVD OSLO",
            "navnelinje1": "TENOR TESTFIRMA AS AVD OSLO"
          },
          "type": "Virksomhet",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "BEDR"
              }
            ],
            "forretningsadresser": [
              {
                "adresselinje1": "Testveien 1",
                "postnummer": "0170",
                "poststed": "OSLO",
                "landkode": "NO"
              }
            ]
          },
          "inngaarIJuridiskEnheter": [
            {
              "organisasjonsnummer": "123456789"
            }
          ]
        }
    """.trimIndent()

    @Language("JSON")
    val VIRKSOMHET_SLETTET = """
        {
          "organisasjonsnummer": "987654322",
          "navn": {
            "sammensattnavn": "TENOR TESTFIRMA AS AVD NEDLAGT",
            "navnelinje1": "TENOR TESTFIRMA AS AVD NEDLAGT"
          },
          "type": "Virksomhet",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "BEDR"
              }
            ],
            "opphoersdato": "2023-12-30"
          },
          "inngaarIJuridiskEnheter": [
            {
              "organisasjonsnummer": "123456789"
            }
          ]
        }
    """.trimIndent()

    @Language("JSON")
    val VIRKSOMHET_UTEN_JURIDISK_ENHET = """
        {
          "organisasjonsnummer": "987654321",
          "navn": {
            "sammensattnavn": "TENOR TESTFIRMA AS AVD OSLO",
            "navnelinje1": "TENOR TESTFIRMA AS AVD OSLO"
          },
          "type": "Virksomhet",
          "organisasjonDetaljer": {
            "forretningsadresser": []
          },
          "inngaarIJuridiskEnheter": []
        }
    """.trimIndent()

    @Language("JSON")
    val VIRKSOMHET_MED_ORGANISASJONSLEDD = """
        {
          "organisasjonsnummer": "987654323",
          "navn": {
            "sammensattnavn": "TENOR TESTETAT UNDERAVDELING",
            "navnelinje1": "TENOR TESTETAT UNDERAVDELING"
          },
          "type": "Virksomhet",
          "organisasjonDetaljer": {
            "enhetstyper": [
              {
                "enhetstype": "BEDR"
              }
            ]
          },
          "bestaarAvOrganisasjonsledd": [
            {
              "organisasjonsledd": {
                "organisasjonsnummer": "111111111",
                "type": "Organisasjonsledd",
                "navn": {
                  "sammensattnavn": "TENOR TESTETAT",
                  "navnelinje1": "TENOR TESTETAT"
                },
                "inngaarIJuridiskEnheter": [
                  {
                    "organisasjonsnummer": "123456789"
                  }
                ]
              }
            }
          ]
        }
    """.trimIndent()

    @Language("JSON")
    val FINN_ORGANISASJON = """
        {
          "organisasjonSammendrag": [
            {
              "organisasjonsnummer": "123456789",
              "enhetstype": "AS",
              "sammensattnavn": "TENOR TESTFIRMA AS",
              "navnelinje1": "TENOR TESTFIRMA AS",
              "adresselinje1": "Testveien 1",
              "postnummer": "0170",
              "poststed": "OSLO",
              "landkode": "NO"
            },
            {
              "organisasjonsnummer": "987654321",
              "enhetstype": "BEDR",
              "sammensattnavn": "TENOR TESTFIRMA AS AVD OSLO",
              "navnelinje1": "TENOR TESTFIRMA AS AVD OSLO",
              "juridiskEnhetOrganisasjonsnummer": "123456789",
              "adresselinje1": "Testveien 1",
              "postnummer": "0170",
              "poststed": "OSLO",
              "landkode": "NO"
            },
            {
              "organisasjonsnummer": "987654322",
              "enhetstype": "AAFY",
              "sammensattnavn": "TENOR TESTFORENING AVD OSLO",
              "navnelinje1": "TENOR TESTFORENING AVD OSLO",
              "juridiskEnhetOrganisasjonsnummer": "123456789",
              "adresselinje1": "Testveien 1",
              "postnummer": "0170",
              "poststed": "OSLO",
              "landkode": "NO"
            }
          ]
        }
    """.trimIndent()

    @Language("JSON")
    val FINN_ORGANISASJON_INGEN_TREFF = """
        {
          "organisasjonSammendrag": []
        }
    """.trimIndent()
}
