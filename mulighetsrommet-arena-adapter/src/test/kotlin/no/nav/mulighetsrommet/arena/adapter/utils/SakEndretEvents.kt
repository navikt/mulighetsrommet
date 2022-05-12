package no.nav.mulighetsrommet.arena.adapter.utils

val sakInsert = """
    {
      "table": "SIAMO.SAK",
      "op_type": "I",
      "op_ts": "2022-05-12 03:00:21.000000",
      "current_ts": "2022-05-12 03:00:25.253000",
      "pos": "00000000340266991496",
      "after": {
        "SAK_ID": 12345678,
        "SAKSKODE": "AA",
        "REG_DATO": "2022-05-12 03:00:16",
        "REG_USER": "AKN1416",
        "MOD_DATO": "2022-05-12 03:00:16",
        "MOD_USER": "AKN1416",
        "TABELLNAVNALIAS": "PERS",
        "OBJEKT_ID": 4885885,
        "AAR": 2022,
        "LOPENRSAK": 49011,
        "DATO_AVSLUTTET": null,
        "SAKSTATUSKODE": "AKTIV",
        "ARKIVNOKKEL": null,
        "AETATENHET_ARKIV": null,
        "ARKIVHENVISNING": null,
        "BRUKERID_ANSVARLIG": "AKN1416",
        "AETATENHET_ANSVARLIG": "1416",
        "OBJEKT_KODE": null,
        "STATUS_ENDRET": "2022-05-12 03:00:16",
        "PARTISJON": null,
        "ER_UTLAND": "N"
      }
    }
""".trimIndent()

val sakUpdate1 = """
    {
      "table": "SIAMO.SAK",
      "op_type": "U",
      "op_ts": "2022-05-12 01:00:27.000000",
      "current_ts": "2022-05-12 01:00:33.388001",
      "pos": "00000000340264525650",
      "before": {
        "SAK_ID": 12345678,
        "SAKSKODE": "AA",
        "REG_DATO": "2022-05-12 01:00:16",
        "REG_USER": "ESE0602",
        "MOD_DATO": "2022-05-12 01:00:16",
        "MOD_USER": "ESE0602",
        "TABELLNAVNALIAS": "PERS",
        "OBJEKT_ID": 4885880,
        "AAR": 2020,
        "LOPENRSAK": 1209868,
        "DATO_AVSLUTTET": null,
        "SAKSTATUSKODE": "AKTIV",
        "ARKIVNOKKEL": null,
        "AETATENHET_ARKIV": null,
        "ARKIVHENVISNING": null,
        "BRUKERID_ANSVARLIG": "ESE0602",
        "AETATENHET_ANSVARLIG": "0602",
        "OBJEKT_KODE": null,
        "STATUS_ENDRET": "2022-05-12 01:00:16",
        "PARTISJON": null,
        "ER_UTLAND": "N"
      },
      "after": {
        "SAK_ID": 12345678,
        "SAKSKODE": "AA",
        "REG_DATO": "2022-05-12 01:00:16",
        "REG_USER": "ESE0602",
        "MOD_DATO": "2020-02-29 01:00:16",
        "MOD_USER": "ESE0602",
        "TABELLNAVNALIAS": "PERS",
        "OBJEKT_ID": 4885880,
        "AAR": 2020,
        "LOPENRSAK": 1209868,
        "DATO_AVSLUTTET": null,
        "SAKSTATUSKODE": "AKTIV",
        "ARKIVNOKKEL": null,
        "AETATENHET_ARKIV": null,
        "ARKIVHENVISNING": null,
        "BRUKERID_ANSVARLIG": "ESE0602",
        "AETATENHET_ANSVARLIG": "0602",
        "OBJEKT_KODE": null,
        "STATUS_ENDRET": "2022-05-12 01:00:16",
        "PARTISJON": null,
        "ER_UTLAND": "N"
      }
    }
""".trimIndent()


val sakUpdate2 = """
        {
          "table": "SIAMO.SAK",
          "op_type": "U",
          "op_ts": "2022-05-12 01:00:27.000000",
          "current_ts": "2022-05-12 01:00:33.388001",
          "pos": "00000000340264525650",
          "before": {
            "SAK_ID": 12312312,
            "SAKSKODE": "AA",
            "REG_DATO": "2022-05-12 01:00:16",
            "REG_USER": "ESE0602",
            "MOD_DATO": "2022-05-12 01:00:16",
            "MOD_USER": "ESE0602",
            "TABELLNAVNALIAS": "PERS",
            "OBJEKT_ID": 4885880,
            "AAR": 2020,
            "LOPENRSAK": 1209868,
            "DATO_AVSLUTTET": null,
            "SAKSTATUSKODE": "AKTIV",
            "ARKIVNOKKEL": null,
            "AETATENHET_ARKIV": null,
            "ARKIVHENVISNING": null,
            "BRUKERID_ANSVARLIG": "ESE0602",
            "AETATENHET_ANSVARLIG": "0602",
            "OBJEKT_KODE": null,
            "STATUS_ENDRET": "2022-05-12 01:00:16",
            "PARTISJON": null,
            "ER_UTLAND": "N"
          },
          "after": {
            "SAK_ID": 12312312,
            "SAKSKODE": "AA",
            "REG_DATO": "2022-05-12 01:00:16",
            "REG_USER": "ESE0602",
            "MOD_DATO": "2020-02-29 01:00:16",
            "MOD_USER": "ESE0602",
            "TABELLNAVNALIAS": "PERS",
            "OBJEKT_ID": 4885880,
            "AAR": 2020,
            "LOPENRSAK": 1209868,
            "DATO_AVSLUTTET": null,
            "SAKSTATUSKODE": "AKTIV",
            "ARKIVNOKKEL": null,
            "AETATENHET_ARKIV": null,
            "ARKIVHENVISNING": null,
            "BRUKERID_ANSVARLIG": "ESE0602",
            "AETATENHET_ANSVARLIG": "0602",
            "OBJEKT_KODE": null,
            "STATUS_ENDRET": "2022-05-12 01:00:16",
            "PARTISJON": null,
            "ER_UTLAND": "N"
          }
        }
""".trimIndent()

val sakEndretTopic = listOf(Pair("12345678", sakInsert), Pair("12345678", sakUpdate1), Pair("12312312", sakUpdate2))
