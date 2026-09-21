╔═ ANNULLERING ═╗
{
  "type": "ANNULLERING",
  "payload": {
    "bestillingsnummer": "A-1-1",
    "behandletAv": {
      "type": "NAV_ANSATT",
      "navIdent": "Z123456"
    },
    "behandletTidspunkt": "2025-01-03T00:00:00Z",
    "besluttetAv": {
      "type": "FAGSYSTEM",
      "kilde": "TILTAKSADMINISTRASJON"
    },
    "besluttetTidspunkt": "2025-01-04T00:00:00Z"
  }
}
╔═ BESTILLING med norsk arrangør og behandlet av system, besluttet av nav-ansatt ═╗
{
  "type": "BESTILLING",
  "payload": {
    "bestillingsnummer": "A-1-1",
    "tilskuddstype": "TILTAK_DRIFTSTILSKUDD",
    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
    "arrangor": {
      "type": "NORSK",
      "organisasjonsnummer": "123456789"
    },
    "kostnadssted": "0400",
    "avtalenummer": "avtale-1",
    "belop": 1000,
    "periode": {
      "start": "2025-01-01",
      "slutt": "2025-02-01"
    },
    "behandletAv": {
      "type": "FAGSYSTEM",
      "kilde": "TILTAKSADMINISTRASJON"
    },
    "behandletTidspunkt": "2025-01-01T00:00:00Z",
    "besluttetAv": {
      "type": "NAV_ANSATT",
      "navIdent": "Z123456"
    },
    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
    "valuta": "NOK"
  }
}
╔═ BESTILLING med utenlandsk arrangør ═╗
{
  "type": "BESTILLING",
  "payload": {
    "bestillingsnummer": "E-1-1",
    "tilskuddstype": "TILTAK_DRIFTSTILSKUDD",
    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
    "arrangor": {
      "type": "UTENLANDSK",
      "organisasjonsnummer": "123456789",
      "navn": "Utenlandsk Arrangør AS",
      "gateNavn": "Main Street 1",
      "by": "Stockholm",
      "postNummer": "12345",
      "landKode": "SE"
    },
    "kostnadssted": "0400",
    "avtalenummer": null,
    "belop": 2000,
    "periode": {
      "start": "2025-01-01",
      "slutt": "2025-02-01"
    },
    "behandletAv": {
      "type": "FAGSYSTEM",
      "kilde": "EKSPERTBISTAND"
    },
    "behandletTidspunkt": "2025-01-01T00:00:00Z",
    "besluttetAv": {
      "type": "FAGSYSTEM",
      "kilde": "EKSPERTBISTAND"
    },
    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
    "valuta": "NOK"
  }
}
╔═ FAKTURA med BBan-betalingsinformasjon ═╗
{
  "type": "FAKTURA",
  "payload": {
    "fakturanummer": "A-1-1-1",
    "bestillingsnummer": "A-1-1",
    "betalingsinformasjon": {
      "type": "BBAN",
      "kontonummer": "12345678901",
      "kid": "0004614992"
    },
    "belop": 1000,
    "periode": {
      "start": "2025-01-01",
      "slutt": "2025-02-01"
    },
    "behandletAv": {
      "type": "FAGSYSTEM",
      "kilde": "TILTAKSADMINISTRASJON"
    },
    "behandletTidspunkt": "2025-01-01T00:00:00Z",
    "besluttetAv": {
      "type": "NAV_ANSATT",
      "navIdent": "Z123456"
    },
    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
    "gjorOppBestilling": false,
    "beskrivelse": "En beskrivelse",
    "valuta": "NOK"
  }
}
╔═ FAKTURA med IBan-betalingsinformasjon ═╗
{
  "type": "FAKTURA",
  "payload": {
    "fakturanummer": "A-1-1-1",
    "bestillingsnummer": "A-1-1",
    "betalingsinformasjon": {
      "type": "IBAN",
      "bic": "DABANO22",
      "iban": "NO9386011117947",
      "bankNavn": "DNB",
      "bankLandKode": "NO"
    },
    "belop": 1000,
    "periode": {
      "start": "2025-01-01",
      "slutt": "2025-02-01"
    },
    "behandletAv": {
      "type": "FAGSYSTEM",
      "kilde": "TILTAKSADMINISTRASJON"
    },
    "behandletTidspunkt": "2025-01-01T00:00:00Z",
    "besluttetAv": {
      "type": "NAV_ANSATT",
      "navIdent": "Z123456"
    },
    "besluttetTidspunkt": "2025-01-02T00:00:00Z",
    "gjorOppBestilling": true,
    "beskrivelse": null,
    "valuta": "NOK"
  }
}
╔═ GJOR_OPP_BESTILLING ═╗
{
  "type": "GJOR_OPP_BESTILLING",
  "payload": {
    "bestillingsnummer": "A-1-1",
    "behandletAv": {
      "type": "FAGSYSTEM",
      "kilde": "TILTAKSADMINISTRASJON"
    },
    "behandletTidspunkt": "2025-01-01T00:00:00Z",
    "besluttetAv": {
      "type": "NAV_ANSATT",
      "navIdent": "Z123456"
    },
    "besluttetTidspunkt": "2025-01-02T00:00:00Z"
  }
}
╔═ [end of file] ═╗
