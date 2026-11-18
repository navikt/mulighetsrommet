╔═ enkeltplass med anskaffelse ═╗
{
  "type": "TiltaksgjennomforingV2.Enkeltplass",
  "id": "00000000-0000-0000-0000-000000000002",
  "lopenummer": "2026/456",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "ENKELTPLASS",
  "pameldingType": "DIREKTE_VEDTAK",
  "prisinformasjon": {
    "type": "Anskaffelse",
    "pris": 12500
  }
}
╔═ enkeltplass med tilskudd ═╗
{
  "type": "TiltaksgjennomforingV2.Enkeltplass",
  "id": "00000000-0000-0000-0000-000000000002",
  "lopenummer": "2026/456",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "ENKELTPLASS",
  "pameldingType": "DIREKTE_VEDTAK",
  "prisinformasjon": {
    "type": "Tilskudd",
    "tilskudd": {
      "SKOLEPENGER": 12500,
      "STUDIEREISE": 2300,
      "EKSAMENSGEBYR": 450,
      "SEMESTERAVGIFT": 600,
      "INTEGRERT_BOTILBUD": 8000
    },
    "tilleggsopplysninger": "Inkluderer læremidler"
  }
}
╔═ enkeltplass med tilskudd uten tilleggsopplysninger ═╗
{
  "type": "TiltaksgjennomforingV2.Enkeltplass",
  "id": "00000000-0000-0000-0000-000000000002",
  "lopenummer": "2026/456",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "ENKELTPLASS",
  "pameldingType": "DIREKTE_VEDTAK",
  "prisinformasjon": {
    "type": "Tilskudd",
    "tilskudd": {},
    "tilleggsopplysninger": null
  }
}
╔═ enkeltplass uten kostnader ═╗
{
  "type": "TiltaksgjennomforingV2.Enkeltplass",
  "id": "00000000-0000-0000-0000-000000000002",
  "lopenummer": "2026/456",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "ENKELTPLASS",
  "pameldingType": "DIREKTE_VEDTAK",
  "prisinformasjon": {
    "type": "IngenKostnader",
    "tilleggsopplysninger": "Offentlig finansiert opplæring"
  }
}
╔═ enkeltplass uten løpenummer og prisinformasjon ═╗
{
  "type": "TiltaksgjennomforingV2.Enkeltplass",
  "id": "00000000-0000-0000-0000-000000000002",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "ENKELTPLASS",
  "pameldingType": "DIREKTE_VEDTAK"
}
╔═ gruppe med alle felter ═╗
{
  "type": "TiltaksgjennomforingV2.Gruppe",
  "id": "00000000-0000-0000-0000-000000000001",
  "lopenummer": "2026/123",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "FELLES",
  "pameldingType": "TRENGER_GODKJENNING",
  "navn": "Norskopplæring",
  "startDato": "2026-03-01",
  "sluttDato": "2026-06-30",
  "tilgjengeligForArrangorFraOgMedDato": "2026-02-15",
  "apentForPamelding": true,
  "antallPlasser": 15,
  "deltidsprosent": 80.5,
  "oppmoteSted": "Tromsø"
}
╔═ gruppe med nullable felter ═╗
{
  "type": "TiltaksgjennomforingV2.Gruppe",
  "id": "00000000-0000-0000-0000-000000000001",
  "lopenummer": "2026/123",
  "opprettetTidspunkt": "2026-01-02T10:15:30Z",
  "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
  "tiltakskode": "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
  "arrangor": {
    "organisasjonsnummer": "999999999"
  },
  "status": "GJENNOMFORES",
  "oppstart": "FELLES",
  "pameldingType": "TRENGER_GODKJENNING",
  "navn": "Norskopplæring",
  "startDato": "2026-03-01",
  "sluttDato": null,
  "tilgjengeligForArrangorFraOgMedDato": null,
  "apentForPamelding": true,
  "antallPlasser": 15,
  "deltidsprosent": 80.5,
  "oppmoteSted": null
}
╔═ [end of file] ═╗
