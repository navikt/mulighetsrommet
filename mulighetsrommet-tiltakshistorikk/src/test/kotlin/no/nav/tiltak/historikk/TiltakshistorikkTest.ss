╔═ returnerer Arena-deltakelse som JSON ═╗
{
  "historikk": [
    {
      "type": "ArenaDeltakelse",
      "norskIdent": "10101010100",
      "startDato": "2024-01-01",
      "sluttDato": "2024-12-31",
      "opprettetTidspunkt": "2024-01-01T12:00:00Z",
      "oppdatertTidspunkt": "2024-02-01T12:00:00Z",
      "id": "6a7c2961-df09-420b-ab05-65d34de7c2cf",
      "tittel": "Arbeidsmarkedsopplæring (enkeltplass) hos Arrangør",
      "arenaId": 123,
      "status": "GJENNOMFORES",
      "tiltakstype": {
        "tiltakskode": "ENKELAMO",
        "navn": "Arbeidsmarkedsopplæring (enkeltplass)"
      },
      "gjennomforing": {
        "id": "169dd79f-e65c-4454-bef4-fb9b352424c8",
        "navn": "Enkelt-AMO hos Joblearn",
        "deltidsprosent": 100.0
      },
      "arrangor": {
        "hovedenhet": null,
        "underenhet": {
          "organisasjonsnummer": "987654321",
          "navn": "Arrangør"
        }
      },
      "deltidsprosent": 100.0,
      "dagerPerUke": 5.0,
      "opphav": "ARENA"
    }
  ]
}
╔═ returnerer Komet-deltakelse som JSON ═╗
{
  "historikk": [
    {
      "type": "TeamKometDeltakelse",
      "norskIdent": "10101010100",
      "startDato": null,
      "sluttDato": null,
      "opprettetTidspunkt": "2021-12-31T23:00:00Z",
      "oppdatertTidspunkt": "2021-12-31T23:00:00Z",
      "id": "d702a2af-b091-41bb-b54b-c0e19312baee",
      "tittel": "Arbeidsmarkedsopplæring (gruppe) hos Arrangør",
      "status": {
        "type": "VENTER_PA_OPPSTART",
        "aarsak": null,
        "opprettetTidspunkt": "2022-01-01T00:00"
      },
      "tiltakstype": {
        "tiltakskode": "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
        "navn": "Arbeidsmarkedsopplæring (gruppe)"
      },
      "gjennomforing": {
        "id": "86a263fa-1f30-4c0e-af64-2e7015e6daed",
        "navn": "Gruppe AMO",
        "deltidsprosent": 80.0
      },
      "arrangor": {
        "hovedenhet": null,
        "underenhet": {
          "organisasjonsnummer": "987654321",
          "navn": "Arrangør"
        }
      },
      "deltidsprosent": 50.0,
      "dagerPerUke": 2.5,
      "opphav": "TEAM_KOMET"
    }
  ]
}
╔═ returnerer Team Tiltak-avtale som JSON ═╗
{
  "historikk": [
    {
      "type": "TeamTiltakAvtale",
      "norskIdent": "10101010100",
      "startDato": "2024-01-01",
      "sluttDato": "2024-12-31",
      "opprettetTidspunkt": "2024-01-01T12:00:00Z",
      "oppdatertTidspunkt": "2024-02-01T12:00:00Z",
      "id": "065e130b-5fc0-4df3-8483-165cd59c26d8",
      "tittel": "Arbeidstrening hos Arbeidsgiver",
      "tiltakstype": {
        "tiltakskode": "ARBEIDSTRENING",
        "navn": "Arbeidstrening"
      },
      "status": "GJENNOMFORES",
      "stillingsprosent": 100.0,
      "dagerPerUke": 5.0,
      "arbeidsgiver": {
        "organisasjonsnummer": "876543210",
        "navn": "Arbeidsgiver"
      },
      "opphav": "TEAM_TILTAK"
    }
  ]
}
╔═ returnerer tom historikk når forespurte identer ikke har deltakelser ═╗
{
  "historikk": []
}
╔═ [end of file] ═╗
