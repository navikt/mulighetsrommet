package no.nav.mulighetsrommet.arena.adapter.utils

/**
 * Litt tilfeldig uttrek av events på topics fra kafka-manager som Strings.
 */

val tiltakEndretMentor = """
{
  "table": "SIAMO.TILTAK",
  "op_type": "I",
  "op_ts": "2021-11-18 11:32:21.953855",
  "current_ts": "2021-11-18 12:48:35.208001",
  "pos": "00000000000000038901",
  "after": {
    "TILTAKSNAVN": "2-årig opplæringstiltak",
    "TILTAKSGRUPPEKODE": "UTFAS",
    "REG_DATO": "2015-12-30 08:42:06",
    "REG_USER": "SIAMO",
    "MOD_DATO": "2021-04-09 09:21:26",
    "MOD_USER": "SIAMO",
    "TILTAKSKODE": "MENTOR",
    "DATO_FRA": "2016-01-01 00:00:00",
    "DATO_TIL": "2019-06-30 00:00:00",
    "AVSNITT_ID_GENERELT": null,
    "STATUS_BASISYTELSE": "J",
    "ADMINISTRASJONKODE": "IND",
    "STATUS_KOPI_TILSAGN": "J",
    "ARKIVNOKKEL": "533",
    "STATUS_ANSKAFFELSE": "J",
    "MAKS_ANT_PLASSER": null,
    "MAKS_ANT_SOKERE": null,
    "STATUS_FAST_ANT_PLASSER": null,
    "STATUS_SJEKK_ANT_DELTAKERE": null,
    "STATUS_KALKULATOR": "N",
    "RAMMEAVTALE": "SKAL",
    "OPPLAERINGSGRUPPE": "UTD",
    "HANDLINGSPLAN": "TIL",
    "STATUS_SLUTTDATO": "J",
    "MAKS_PERIODE": 24,
    "STATUS_MELDEPLIKT": null,
    "STATUS_VEDTAK": "J",
    "STATUS_IA_AVTALE": "N",
    "STATUS_TILLEGGSSTONADER": "J",
    "STATUS_UTDANNING": "N",
    "AUTOMATISK_TILSAGNSBREV": "N",
    "STATUS_BEGRUNNELSE_INNSOKT": "J",
    "STATUS_HENVISNING_BREV": "N",
    "STATUS_KOPIBREV": "N"
  }
}
""".trimIndent()

val tiltakEndretJobbklubb = """
{
  "table": "SIAMO.TILTAK",
  "op_type": "I",
  "op_ts": "2021-11-18 11:32:21.953855",
  "current_ts": "2021-11-18 12:48:35.189000",
  "pos": "00000000000000018863",
  "after": {
    "TILTAKSNAVN": "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)",
    "TILTAKSGRUPPEKODE": "OPPFOLG",
    "REG_DATO": "2021-01-01 13:49:57",
    "REG_USER": "SKRIPT",
    "MOD_DATO": "2021-04-09 09:21:26",
    "MOD_USER": "SIAMO",
    "TILTAKSKODE": "DIGIOPPARB",
    "DATO_FRA": "2021-01-01 00:00:00",
    "DATO_TIL": "2099-01-01 00:00:00",
    "AVSNITT_ID_GENERELT": null,
    "STATUS_BASISYTELSE": "J",
    "ADMINISTRASJONKODE": "AMO",
    "STATUS_KOPI_TILSAGN": "N",
    "ARKIVNOKKEL": "529",
    "STATUS_ANSKAFFELSE": "J",
    "MAKS_ANT_PLASSER": null,
    "MAKS_ANT_SOKERE": null,
    "STATUS_FAST_ANT_PLASSER": "N",
    "STATUS_SJEKK_ANT_DELTAKERE": "N",
    "STATUS_KALKULATOR": "N",
    "RAMMEAVTALE": "SKAL",
    "OPPLAERINGSGRUPPE": null,
    "HANDLINGSPLAN": "SOK",
    "STATUS_SLUTTDATO": "N",
    "MAKS_PERIODE": null,
    "STATUS_MELDEPLIKT": null,
    "STATUS_VEDTAK": "N",
    "STATUS_IA_AVTALE": "N",
    "STATUS_TILLEGGSSTONADER": "J",
    "STATUS_UTDANNING": "N",
    "AUTOMATISK_TILSAGNSBREV": "N",
    "STATUS_BEGRUNNELSE_INNSOKT": "N",
    "STATUS_HENVISNING_BREV": "N",
    "STATUS_KOPIBREV": "N"
  }
}
""".trimIndent()

val tiltakEndretJobbklubbUpdate = """
{
  "table": "SIAMO.TILTAK",
  "op_type": "U",
  "op_ts": "2021-11-18 11:32:21.953855",
  "current_ts": "2021-11-18 12:48:35.189000",
  "pos": "00000000000000018863",
  "after": {
    "TILTAKSNAVN": "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb) OPPDATERT!!!",
    "TILTAKSGRUPPEKODE": "OPPFOLG",
    "REG_DATO": "2021-01-01 13:49:57",
    "REG_USER": "SKRIPT",
    "MOD_DATO": "2021-04-09 09:21:26",
    "MOD_USER": "SIAMO",
    "TILTAKSKODE": "DIGIOPPARB",
    "DATO_FRA": "2021-01-01 00:00:00",
    "DATO_TIL": "2025-01-01 00:00:00",
    "AVSNITT_ID_GENERELT": null,
    "STATUS_BASISYTELSE": "J",
    "ADMINISTRASJONKODE": "AMO",
    "STATUS_KOPI_TILSAGN": "N",
    "ARKIVNOKKEL": "529",
    "STATUS_ANSKAFFELSE": "J",
    "MAKS_ANT_PLASSER": null,
    "MAKS_ANT_SOKERE": null,
    "STATUS_FAST_ANT_PLASSER": "N",
    "STATUS_SJEKK_ANT_DELTAKERE": "N",
    "STATUS_KALKULATOR": "N",
    "RAMMEAVTALE": "SKAL",
    "OPPLAERINGSGRUPPE": null,
    "HANDLINGSPLAN": "SOK",
    "STATUS_SLUTTDATO": "N",
    "MAKS_PERIODE": null,
    "STATUS_MELDEPLIKT": null,
    "STATUS_VEDTAK": "N",
    "STATUS_IA_AVTALE": "N",
    "STATUS_TILLEGGSSTONADER": "J",
    "STATUS_UTDANNING": "N",
    "AUTOMATISK_TILSAGNSBREV": "N",
    "STATUS_BEGRUNNELSE_INNSOKT": "N",
    "STATUS_HENVISNING_BREV": "N",
    "STATUS_KOPIBREV": "N"
  }
}
""".trimIndent()

val tiltakEndretTopic = listOf(Pair("MENTOR", tiltakEndretMentor), Pair("DIGIOPPARB", tiltakEndretJobbklubb), Pair("DIGIOPPARB", tiltakEndretJobbklubbUpdate))
