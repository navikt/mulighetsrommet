package no.nav.mulighetsrommet.api.amo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language

class OpplaringKategoriseringMapperTest : FunSpec({
    val dbListener = extension(ApiDatabaseTestListener(databaseConfig))
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
    val inserts = listOf(
        UTDANNINGSPROGRAM_SQL,
        UTDANNING_SQL,
    )
    beforeSpec {
        MulighetsrommetTestDomain {
            inserts.forEach {
                this.session.execute(queryOf(it))
            }
        }.initialize(dbListener.db)
    }

    test("STUDIESPESIALISERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        jsonPrettyPrint.encodeToString(service.from(Tiltakskode.STUDIESPESIALISERING)) shouldBeEqual STUDIESPESIALISERING_JSON
    }
    test("NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        jsonPrettyPrint.encodeToString(service.from(Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV)) shouldBeEqual NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV_JSON
    }

    xtest("FAG_OG_YRKESOPPLAERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        jsonPrettyPrint.encodeToString(service.from(Tiltakskode.FAG_OG_YRKESOPPLAERING)) shouldBeEqual FAG_OG_YRKESOPPLAERING_JSON
    }

    test("ARBEIDSMARKEDSOPPLAERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        jsonPrettyPrint.encodeToString(service.from(Tiltakskode.ARBEIDSMARKEDSOPPLAERING)) shouldBeEqual ARBEIDSMARKEDSOPPLAERING_JSON
    }
    test("GRUPPE_ARBEIDSMARKEDSOPPLAERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        jsonPrettyPrint.encodeToString(service.from(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING)) shouldBeEqual GRUPPE_ARBEIDSMARKEDSOPPLAERING_JSON
    }
})

const val STUDIESPESIALISERING_JSON = """{
  "tiltakskode": "STUDIESPESIALISERING",
  "alternativer": []
}"""

const val NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV_JSON = """{
  "tiltakskode": "NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV",
  "alternativer": [
    {
      "type": "Verdigruppe",
      "id": null,
      "visningsnavn": "Kurstype",
      "tooltip": null,
      "pakrevd": true,
      "representerer": "kurstypeId",
      "seleksjonstype": "ENKELTVALG",
      "alternativer": [
        {
          "id": "347ef4a1-be8c-47b6-8e67-54244b648a9f",
          "visningsnavn": "FOV (Forberedende opplæring for voksne)"
        },
        {
          "id": "19544ff4-25e5-4925-b942-6109b2a98552",
          "visningsnavn": "Grunnleggende ferdigheter"
        },
        {
          "id": "8e294221-bf60-466a-96bd-7c59c338ee5e",
          "visningsnavn": "Norskopplæring"
        }
      ]
    }
  ]
}"""

const val FAG_OG_YRKESOPPLAERING_JSON = """{
  "tiltakskode": "FAG_OG_YRKESOPPLAERING",
  "alternativer": [
    {
      "type": "Gruppe",
      "id": null,
      "visningsnavn": "Utdanningsprogram",
      "representerer": "utdanningsprogram",
      "pakrevd": true,
      "alternativer": [
        {
          "type": "Gruppe",
          "id": "1390a963-e9b2-4677-bb87-243f4638b7a1",
          "visningsnavn": "Bygg- og anleggsteknikk",
          "representerer": null,
          "pakrevd": false,
          "alternativer": [
            {
              "type": "Verdigruppe",
              "id": null,
              "visningsnavn": "Lærefag",
              "tooltip": null,
              "pakrevd": true,
              "representerer": "larefag",
              "seleksjonstype": "FLERVALG",
              "alternativer": [
                {
                  "id": "d02ffbea-7f0e-42ff-91a0-88d56277699d",
                  "visningsnavn": "Banemontørfaget"
                },
                {
                  "id": "32fff4d1-ef8f-4990-8184-7a7b34febe3b",
                  "visningsnavn": "Byggdrifterfaget"
                },
                {
                  "id": "c8ac69fa-6ba6-494f-95e1-6ec9be06086d",
                  "visningsnavn": "Fjell- og bergverksfaget"
                },
                {
                  "id": "43b8c872-5a7e-4fa2-8494-dd3258d60ad0",
                  "visningsnavn": "Glassfaget"
                },
                {
                  "id": "a2287ed9-6349-47ff-9cbe-5bdbb0b6cf2c",
                  "visningsnavn": "Vei- og anleggsfaget"
                }
              ]
            }
          ]
        },
        {
          "type": "Gruppe",
          "id": "1626096d-f1ac-4c34-aa93-741503bc5584",
          "visningsnavn": "Håndverk, design og produktutvikling",
          "representerer": null,
          "pakrevd": false,
          "alternativer": [
            {
              "type": "Verdigruppe",
              "id": null,
              "visningsnavn": "Lærefag",
              "tooltip": null,
              "pakrevd": true,
              "representerer": "larefag",
              "seleksjonstype": "FLERVALG",
              "alternativer": [
                {
                  "id": "649e9784-0422-4216-9cea-5af581a5c9c4",
                  "visningsnavn": "Bunadstilvirkerfaget"
                },
                {
                  "id": "74db0d0a-549f-4421-b30d-a11a34ede018",
                  "visningsnavn": "Gjørtlerfaget"
                },
                {
                  "id": "7a7a7c50-4800-4e97-be97-f4a87216c8f5",
                  "visningsnavn": "Glassblåserfaget"
                },
                {
                  "id": "c1092785-1fb1-4aee-ad96-1b8a8069bb1e",
                  "visningsnavn": "Herreskredderfaget"
                },
                {
                  "id": "7d9403b6-7883-4322-b340-c702e143c802",
                  "visningsnavn": "Kostymesyerfaget"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}"""

const val ARBEIDSMARKEDSOPPLAERING_JSON = """{
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "alternativer": [
    {
      "type": "Verdigruppe",
      "id": null,
      "visningsnavn": "Bransje",
      "tooltip": {
        "type": "FlereUtlistinger",
        "liste": [
          {
            "tittel": "Barne- og ungdomsarbeid",
            "innhold": [
              "Skoleassistenter",
              "Barnehage- og skolefritidsassistenter"
            ]
          },
          {
            "tittel": "Butikk- og salgsarbeid",
            "innhold": [
              "Butikkarbeid",
              "Annet salgsarbeid"
            ]
          },
          {
            "tittel": "Bygg og anlegg",
            "innhold": [
              "Rørleggere",
              "Snekkere og tømrere",
              "Elektrikere",
              "Andre bygningsarbeidere",
              "Anleggsarbeidere",
              "Hjelpearbeidere innen bygg og anlegg",
              "Mellomledere innen bygg og anlegg"
            ]
          },
          {
            "tittel": "Helse, pleie og omsorg",
            "innhold": [
              "Omsorgs- og pleiearbeidere",
              "Annet helsepersonell",
              "Mellomledere innen helse, pleie og omsorg"
            ]
          },
          {
            "tittel": "Industriarbeid",
            "innhold": [
              "Mekanikere",
              "Prosess- og maskinoperatører",
              "Næringsmiddelarbeid",
              "Automatikere og elektriske montører",
              "Andre håndverkere",
              "Hjelpearbeid innen industrien",
              "Mellomledere innen industriarbeid"
            ]
          },
          {
            "tittel": "Ingeniør- og IKT-fag",
            "innhold": [
              "Andre naturvitenskapelige yrker",
              "Ikt-yrker",
              "Ingeniører og teknikere"
            ]
          },
          {
            "tittel": "Kontorarbeid",
            "innhold": [
              "Lavere saksbehandlere innen offentlig administrasjon",
              "Sekretærer",
              "Økonomi- og kontormedarbeidere",
              "Lager- og transportmedarbeidere",
              "Resepsjonister og sentralbordoperatører",
              "Andre funksjonærer"
            ]
          },
          {
            "tittel": "Reiseliv, servering og transport",
            "innhold": [
              "Maritime yrker",
              "Førere av transportmidler",
              "Reiseledere, guider og reisebyråmedarbeidere",
              "Konduktører og kabinpersonale",
              "Kokker",
              "Hovmestere, servitører og hjelpepersonell",
              "Mellomledere innen reiseliv og transport"
            ]
          },
          {
            "tittel": "Serviceyrker og annet arbeid",
            "innhold": [
              "Yrker innen politi, brannvesen, toll og forsvar",
              "Velvære",
              "Rengjøring",
              "Vakthold og vaktmestere",
              "Annet arbeid",
              "Yrker innen kunst, sport og kultur"
            ]
          }
        ]
      },
      "pakrevd": true,
      "representerer": "bransjeId",
      "seleksjonstype": "ENKELTVALG",
      "alternativer": [
        {
          "id": "14886bad-a495-420a-9bae-d33e2d88041a",
          "visningsnavn": "Barne- og ungdomsarbeid"
        },
        {
          "id": "e6749d6c-aacf-452d-baf2-d5fb5021912b",
          "visningsnavn": "Butikk- og salgsarbeid"
        },
        {
          "id": "d9b1c8e0-1c3a-4f5b-9c2e-1a2b3c4d5e6f",
          "visningsnavn": "Bygg og anlegg"
        },
        {
          "id": "82bd7ce0-70f1-448b-8773-9015dea613e7",
          "visningsnavn": "Helse, pleie og omsorg"
        },
        {
          "id": "4733d7ef-d106-47a4-b335-bfd132c8ad31",
          "visningsnavn": "Industriarbeid"
        },
        {
          "id": "d04dff0d-fdca-4839-9bdc-44c722af5d6f",
          "visningsnavn": "Ingeniør- og IKT-fag"
        },
        {
          "id": "a86c1f7a-47c3-4f69-b138-89341107e0eb",
          "visningsnavn": "Kontorarbeid"
        },
        {
          "id": "c8851a31-6362-4ee2-8989-e5da95726076",
          "visningsnavn": "Reiseliv, servering og transport"
        },
        {
          "id": "47c9d5f0-66ea-4e68-949d-86733346ee80",
          "visningsnavn": "Serviceyrker og annet arbeid"
        },
        {
          "id": "54ccb278-92ea-4835-8566-659e98602905",
          "visningsnavn": "Andre bransjer"
        }
      ]
    },
    {
      "type": "Verdigruppe",
      "id": null,
      "visningsnavn": "Førerkort",
      "tooltip": null,
      "pakrevd": false,
      "representerer": "forerkort",
      "seleksjonstype": "FLERVALG",
      "alternativer": [
        {
          "id": "810fe1c6-56b0-4e00-8ae6-00fb574299e5",
          "visningsnavn": "A - Motorsykkel"
        },
        {
          "id": "c67006e4-2629-4993-a047-92f31b0db557",
          "visningsnavn": "A1 - Lett motorsykkel"
        },
        {
          "id": "ed44bd3a-aedb-4225-a3d8-c8f1b95fec5a",
          "visningsnavn": "A2 - Mellomtung motorsykkel"
        },
        {
          "id": "dee7d6b8-02dc-4b7e-bb3a-fa71cc9248e3",
          "visningsnavn": "AM - Moped"
        },
        {
          "id": "ee66eb0b-d4a8-4527-800a-135dd3c0d422",
          "visningsnavn": "AM 147 - Mopedbil"
        },
        {
          "id": "79d1a970-e8f0-4ecd-8d5e-e7c8d5f3394c",
          "visningsnavn": "B - Personbil"
        },
        {
          "id": "84a40884-421c-406c-994d-4c4c15ef8bcc",
          "visningsnavn": "B 78 - Personbil med automatgir"
        },
        {
          "id": "cdbebefc-2cec-48d0-9c8e-bd464e56cfaa",
          "visningsnavn": "BE - Personbil med tilhenger"
        },
        {
          "id": "e3fcf1f7-1f20-4fca-bad5-422b7ee0418f",
          "visningsnavn": "C - Lastebil"
        },
        {
          "id": "c65936e4-479f-4c84-b106-6c9ec0cf9aee",
          "visningsnavn": "C1 - Lett lastebil"
        },
        {
          "id": "69f88a08-e2de-461f-9258-4f8be546104a",
          "visningsnavn": "C1E - Lett lastebil med tilhenger"
        },
        {
          "id": "9a85cdeb-2f6d-44f6-bef2-2add850f7b27",
          "visningsnavn": "CE - Lastebil med tilhenger"
        },
        {
          "id": "e637320c-a5f0-4f7d-ad44-0a7c4654b4c2",
          "visningsnavn": "D - Buss"
        },
        {
          "id": "5d890e23-6800-4574-a05d-24ca81f35a2a",
          "visningsnavn": "D1 - Minibuss"
        },
        {
          "id": "34d00562-f382-4027-953d-2b6f6bb7e0e5",
          "visningsnavn": "D1E - Minibuss med tilhenger"
        },
        {
          "id": "a7376d16-b0da-4140-8e67-c589be2c0ea2",
          "visningsnavn": "DE - Buss med tilhenger"
        },
        {
          "id": "5b1e1732-a5e8-45ca-955f-548c65d11065",
          "visningsnavn": "S - Snøscooter"
        },
        {
          "id": "53896c05-7650-48ed-bf23-54ae78794eba",
          "visningsnavn": "T - Traktor"
        }
      ]
    },
    {
      "type": "VerdigruppeSok",
      "id": null,
      "visningsnavn": "Sertifiseringer",
      "pakrevd": false,
      "representerer": "sertifiseringer",
      "seleksjonstype": "FLERVALG",
      "kilde": "JANZZ_SERTIFISERING"
    }
  ]
}"""

const val GRUPPE_ARBEIDSMARKEDSOPPLAERING_JSON = """{
  "tiltakskode": "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
  "alternativer": [
    {
      "type": "Gruppe",
      "id": null,
      "visningsnavn": "Kurstype",
      "representerer": "kurstypeId",
      "pakrevd": true,
      "alternativer": [
        {
          "type": "Gruppe",
          "id": "8c439235-4363-4137-859e-bfa33b0e8f2d",
          "visningsnavn": "Bransje og yrkesrettet",
          "representerer": null,
          "pakrevd": false,
          "alternativer": [
            {
              "type": "Verdigruppe",
              "id": null,
              "visningsnavn": "Bransje",
              "tooltip": {
                "type": "FlereUtlistinger",
                "liste": [
                  {
                    "tittel": "Barne- og ungdomsarbeid",
                    "innhold": [
                      "Skoleassistenter",
                      "Barnehage- og skolefritidsassistenter"
                    ]
                  },
                  {
                    "tittel": "Butikk- og salgsarbeid",
                    "innhold": [
                      "Butikkarbeid",
                      "Annet salgsarbeid"
                    ]
                  },
                  {
                    "tittel": "Bygg og anlegg",
                    "innhold": [
                      "Rørleggere",
                      "Snekkere og tømrere",
                      "Elektrikere",
                      "Andre bygningsarbeidere",
                      "Anleggsarbeidere",
                      "Hjelpearbeidere innen bygg og anlegg",
                      "Mellomledere innen bygg og anlegg"
                    ]
                  },
                  {
                    "tittel": "Helse, pleie og omsorg",
                    "innhold": [
                      "Omsorgs- og pleiearbeidere",
                      "Annet helsepersonell",
                      "Mellomledere innen helse, pleie og omsorg"
                    ]
                  },
                  {
                    "tittel": "Industriarbeid",
                    "innhold": [
                      "Mekanikere",
                      "Prosess- og maskinoperatører",
                      "Næringsmiddelarbeid",
                      "Automatikere og elektriske montører",
                      "Andre håndverkere",
                      "Hjelpearbeid innen industrien",
                      "Mellomledere innen industriarbeid"
                    ]
                  },
                  {
                    "tittel": "Ingeniør- og IKT-fag",
                    "innhold": [
                      "Andre naturvitenskapelige yrker",
                      "Ikt-yrker",
                      "Ingeniører og teknikere"
                    ]
                  },
                  {
                    "tittel": "Kontorarbeid",
                    "innhold": [
                      "Lavere saksbehandlere innen offentlig administrasjon",
                      "Sekretærer",
                      "Økonomi- og kontormedarbeidere",
                      "Lager- og transportmedarbeidere",
                      "Resepsjonister og sentralbordoperatører",
                      "Andre funksjonærer"
                    ]
                  },
                  {
                    "tittel": "Reiseliv, servering og transport",
                    "innhold": [
                      "Maritime yrker",
                      "Førere av transportmidler",
                      "Reiseledere, guider og reisebyråmedarbeidere",
                      "Konduktører og kabinpersonale",
                      "Kokker",
                      "Hovmestere, servitører og hjelpepersonell",
                      "Mellomledere innen reiseliv og transport"
                    ]
                  },
                  {
                    "tittel": "Serviceyrker og annet arbeid",
                    "innhold": [
                      "Yrker innen politi, brannvesen, toll og forsvar",
                      "Velvære",
                      "Rengjøring",
                      "Vakthold og vaktmestere",
                      "Annet arbeid",
                      "Yrker innen kunst, sport og kultur"
                    ]
                  }
                ]
              },
              "pakrevd": true,
              "representerer": "bransjeId",
              "seleksjonstype": "ENKELTVALG",
              "alternativer": [
                {
                  "id": "14886bad-a495-420a-9bae-d33e2d88041a",
                  "visningsnavn": "Barne- og ungdomsarbeid"
                },
                {
                  "id": "e6749d6c-aacf-452d-baf2-d5fb5021912b",
                  "visningsnavn": "Butikk- og salgsarbeid"
                },
                {
                  "id": "d9b1c8e0-1c3a-4f5b-9c2e-1a2b3c4d5e6f",
                  "visningsnavn": "Bygg og anlegg"
                },
                {
                  "id": "82bd7ce0-70f1-448b-8773-9015dea613e7",
                  "visningsnavn": "Helse, pleie og omsorg"
                },
                {
                  "id": "4733d7ef-d106-47a4-b335-bfd132c8ad31",
                  "visningsnavn": "Industriarbeid"
                },
                {
                  "id": "d04dff0d-fdca-4839-9bdc-44c722af5d6f",
                  "visningsnavn": "Ingeniør- og IKT-fag"
                },
                {
                  "id": "a86c1f7a-47c3-4f69-b138-89341107e0eb",
                  "visningsnavn": "Kontorarbeid"
                },
                {
                  "id": "c8851a31-6362-4ee2-8989-e5da95726076",
                  "visningsnavn": "Reiseliv, servering og transport"
                },
                {
                  "id": "47c9d5f0-66ea-4e68-949d-86733346ee80",
                  "visningsnavn": "Serviceyrker og annet arbeid"
                },
                {
                  "id": "54ccb278-92ea-4835-8566-659e98602905",
                  "visningsnavn": "Andre bransjer"
                }
              ]
            },
            {
              "type": "Verdigruppe",
              "id": null,
              "visningsnavn": "Førerkort",
              "tooltip": null,
              "pakrevd": false,
              "representerer": "forerkort",
              "seleksjonstype": "FLERVALG",
              "alternativer": [
                {
                  "id": "810fe1c6-56b0-4e00-8ae6-00fb574299e5",
                  "visningsnavn": "A - Motorsykkel"
                },
                {
                  "id": "c67006e4-2629-4993-a047-92f31b0db557",
                  "visningsnavn": "A1 - Lett motorsykkel"
                },
                {
                  "id": "ed44bd3a-aedb-4225-a3d8-c8f1b95fec5a",
                  "visningsnavn": "A2 - Mellomtung motorsykkel"
                },
                {
                  "id": "dee7d6b8-02dc-4b7e-bb3a-fa71cc9248e3",
                  "visningsnavn": "AM - Moped"
                },
                {
                  "id": "ee66eb0b-d4a8-4527-800a-135dd3c0d422",
                  "visningsnavn": "AM 147 - Mopedbil"
                },
                {
                  "id": "79d1a970-e8f0-4ecd-8d5e-e7c8d5f3394c",
                  "visningsnavn": "B - Personbil"
                },
                {
                  "id": "84a40884-421c-406c-994d-4c4c15ef8bcc",
                  "visningsnavn": "B 78 - Personbil med automatgir"
                },
                {
                  "id": "cdbebefc-2cec-48d0-9c8e-bd464e56cfaa",
                  "visningsnavn": "BE - Personbil med tilhenger"
                },
                {
                  "id": "e3fcf1f7-1f20-4fca-bad5-422b7ee0418f",
                  "visningsnavn": "C - Lastebil"
                },
                {
                  "id": "c65936e4-479f-4c84-b106-6c9ec0cf9aee",
                  "visningsnavn": "C1 - Lett lastebil"
                },
                {
                  "id": "69f88a08-e2de-461f-9258-4f8be546104a",
                  "visningsnavn": "C1E - Lett lastebil med tilhenger"
                },
                {
                  "id": "9a85cdeb-2f6d-44f6-bef2-2add850f7b27",
                  "visningsnavn": "CE - Lastebil med tilhenger"
                },
                {
                  "id": "e637320c-a5f0-4f7d-ad44-0a7c4654b4c2",
                  "visningsnavn": "D - Buss"
                },
                {
                  "id": "5d890e23-6800-4574-a05d-24ca81f35a2a",
                  "visningsnavn": "D1 - Minibuss"
                },
                {
                  "id": "34d00562-f382-4027-953d-2b6f6bb7e0e5",
                  "visningsnavn": "D1E - Minibuss med tilhenger"
                },
                {
                  "id": "a7376d16-b0da-4140-8e67-c589be2c0ea2",
                  "visningsnavn": "DE - Buss med tilhenger"
                },
                {
                  "id": "5b1e1732-a5e8-45ca-955f-548c65d11065",
                  "visningsnavn": "S - Snøscooter"
                },
                {
                  "id": "53896c05-7650-48ed-bf23-54ae78794eba",
                  "visningsnavn": "T - Traktor"
                }
              ]
            },
            {
              "type": "VerdigruppeSok",
              "id": null,
              "visningsnavn": "Sertifiseringer",
              "pakrevd": false,
              "representerer": "sertifiseringer",
              "seleksjonstype": "FLERVALG",
              "kilde": "JANZZ_SERTIFISERING"
            }
          ]
        },
        {
          "type": "Gruppe",
          "id": "347ef4a1-be8c-47b6-8e67-54244b648a9f",
          "visningsnavn": "FOV (Forberedende opplæring for voksne)",
          "representerer": null,
          "pakrevd": false,
          "alternativer": []
        },
        {
          "type": "Gruppe",
          "id": "19544ff4-25e5-4925-b942-6109b2a98552",
          "visningsnavn": "Grunnleggende ferdigheter",
          "representerer": null,
          "pakrevd": false,
          "alternativer": []
        },
        {
          "type": "Gruppe",
          "id": "8e294221-bf60-466a-96bd-7c59c338ee5e",
          "visningsnavn": "Norskopplæring",
          "representerer": null,
          "pakrevd": false,
          "alternativer": []
        },
        {
          "type": "Gruppe",
          "id": "a262e282-2f81-411d-b450-06b7f3d371dc",
          "visningsnavn": "Studiespesialisering",
          "representerer": null,
          "pakrevd": false,
          "alternativer": []
        }
      ]
    }
  ]
}"""

@Language("PostgreSQL")
const val UTDANNINGSPROGRAM_SQL = """
    insert into public.utdanningsprogram (id, navn, nus_koder, programomradekode, utdanningsprogram_type, created_at, updated_at)
values  ('1390a963-e9b2-4677-bb87-243f4638b7a1', 'Bygg- og anleggsteknikk', '{3571}', 'BABAT1----', 'YRKESFAGLIG', '2024-12-09 16:17:31.046728 +00:00', '2026-04-30 04:00:03.294139 +00:00'),
        ('1626096d-f1ac-4c34-aa93-741503bc5584', 'Håndverk, design og produktutvikling', '{3169}', 'DTDTH1----', 'YRKESFAGLIG', '2024-12-09 16:17:31.046728 +00:00', '2026-04-30 04:00:03.294139 +00:00');
"""

@Language("PostgreSQL")
const val UTDANNING_SQL = """
insert into public.utdanning (id, utdanning_id, programomradekode, navn, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, created_at, updated_at, programlop_start, nus_koder)
values  ('c8ac69fa-6ba6-494f-95e1-6ec9be06086d', 'u_fjell_bergverksfag', 'BAFJE3----', 'Fjell- og bergverksfaget', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAANL2----,BAFJE3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1390a963-e9b2-4677-bb87-243f4638b7a1', '{458409}'),
        ('d02ffbea-7f0e-42ff-91a0-88d56277699d', 'u_banemontorfag', 'BABAN3----', 'Banemontørfaget', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAANL2----,BABAN3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1390a963-e9b2-4677-bb87-243f4638b7a1', '{457103}'),
        ('32fff4d1-ef8f-4990-8184-7a7b34febe3b', 'u_byggdrifterfag', 'BABDR3----', 'Byggdrifterfaget', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BABDR3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1390a963-e9b2-4677-bb87-243f4638b7a1', '{457136}'),
        ('43b8c872-5a7e-4fa2-8494-dd3258d60ad0', 'u_glassfag', 'BAGLA3----', 'Glassfaget', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAGLA3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1390a963-e9b2-4677-bb87-243f4638b7a1', '{458303}'),
        ('a2287ed9-6349-47ff-9cbe-5bdbb0b6cf2c', 'u_vei_anleggsfag', 'BAVOA3----', 'Vei- og anleggsfaget', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAANL2----,BAVOA3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1390a963-e9b2-4677-bb87-243f4638b7a1', '{457131}'),
        ('7a7a7c50-4800-4e97-be97-f4a87216c8f5', 'u_glasshandverkerfag', 'DTGBF3----', 'Glassblåserfaget', 'FAGBREV', true, 'GYLDIG', '{DTDTH1----,DTGBF3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1626096d-f1ac-4c34-aa93-741503bc5584', '{416202,458333}'),
        ('649e9784-0422-4216-9cea-5af581a5c9c4', 'u_bunadtilvirkerfag', 'DTBUN3----', 'Bunadstilvirkerfaget', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTSTH2----,DTBUN3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1626096d-f1ac-4c34-aa93-741503bc5584', '{416613,416601}'),
        ('74db0d0a-549f-4421-b30d-a11a34ede018', 'u_gjortlerfag', 'DTGTL3----', 'Gjørtlerfaget', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTGTL3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1626096d-f1ac-4c34-aa93-741503bc5584', '{458902,458901}'),
        ('c1092785-1fb1-4aee-ad96-1b8a8069bb1e', 'u_herreskredderfag', 'DTHSK3----', 'Herreskredderfaget', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTSTH2----,DTHSK3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1626096d-f1ac-4c34-aa93-741503bc5584', '{416614,416603}'),
        ('7d9403b6-7883-4322-b340-c702e143c802', 'u_kostymesyerfag', 'DTKST3----', 'Kostymesyerfaget', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTSTH2----,DTKST3----}', '2024-10-16 12:53:50.912833 +00:00', '2026-04-30 04:00:03.294139 +00:00', '1626096d-f1ac-4c34-aa93-741503bc5584', '{416617,416608}');
"""
