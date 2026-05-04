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

class OpplaringKategoriseringTest : FunSpec({
    val dbListener = extension(ApiDatabaseTestListener(databaseConfig))
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
    beforeSpec {
        MulighetsrommetTestDomain {
            this.session.execute(queryOf(UTDANNINGSPROGRAM_SQL, emptyMap()))
            this.session.execute(queryOf(UTDANNING_SQL, emptyMap()))
        }.initialize(dbListener.db)
    }
    test("STUDIESPESIALISERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        val json =
            jsonPrettyPrint.encodeToString(service.from(Tiltakskode.STUDIESPESIALISERING))
        json shouldBeEqual STUDIESPESIALISERING_JSON
    }
    test("NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        val json =
            jsonPrettyPrint.encodeToString(service.from(Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV))
        json shouldBeEqual NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV_JSON
    }

    test("FAG_OG_YRKESOPPLAERING") {
        val service = OpplaringKategoriseringMapper(dbListener.db)
        val json =
            jsonPrettyPrint.encodeToString(service.from(Tiltakskode.FAG_OG_YRKESOPPLAERING))
        json shouldBeEqual FAG_OG_YRKESOPPLAERING
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
      "id": "00000000-0000-0000-0000-000000000000",
      "visningsnavn": "Kurstype",
      "seleksjonstype": "ENKELTVALG",
      "alternativer": [
        {
          "id": "8e294221-bf60-466a-96bd-7c59c338ee5e",
          "visningsnavn": "Norskopplæring"
        },
        {
          "id": "19544ff4-25e5-4925-b942-6109b2a98552",
          "visningsnavn": "Grunnleggende ferdigheter"
        },
        {
          "id": "19544ff4-25e5-4925-b942-6109b2a98552",
          "visningsnavn": "FOV (Forberedende opplæring for voksne)"
        }
      ]
    }
  ]
}"""

const val FAG_OG_YRKESOPPLAERING = """{
  "tiltakskode": "FAG_OG_YRKESOPPLAERING",
  "alternativer": [
    {
      "type": "Gruppe",
      "id": "00000000-0000-0000-0000-000000000000",
      "visningsnavn": "Utdanningsprogram",
      "alternativer": [
        {
          "type": "Gruppe",
          "id": "1390a963-e9b2-4677-bb87-243f4638b7a1",
          "visningsnavn": "Bygg- og anleggsteknikk",
          "alternativer": [
            {
              "type": "Verdigruppe",
              "id": "00000000-0000-0000-0000-000000000000",
              "visningsnavn": "Lærefag",
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
          "alternativer": [
            {
              "type": "Verdigruppe",
              "id": "00000000-0000-0000-0000-000000000000",
              "visningsnavn": "Lærefag",
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
