import {
  Kontorstruktur,
  KontorstrukturKontortype,
  RegionKostnadssteder,
} from "@tiltaksadministrasjon/api-client";

export const mockEnheter = {
  // Innlandet
  _0425: {
    navn: "Nav Solør",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0425",
    overordnetEnhet: "0400",
  },
  _0402: {
    navn: "Nav Kongsvinger",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0402",
    overordnetEnhet: "0400",
  },
  _0415: {
    navn: "Nav Løten",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0415",
    overordnetEnhet: "0400",
  },
  _0428: {
    navn: "Nav Trysil",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0428",
    overordnetEnhet: "0400",
  },
  _0511: {
    navn: "Nav Lesja - Dovre",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0511",
    overordnetEnhet: "0400",
  },
  _0420: {
    navn: "Nav Eidskog",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0420",
    overordnetEnhet: "0400",
  },
  _0400: {
    navn: "Nav Innlandet",
    enhetsnummer: "0400",
    overordnetEnhet: null,
  },

  // Oslo
  _0313: {
    navn: "Nav St. Hanshaugen",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0313",
    overordnetEnhet: "0300",
  },
  _0330: {
    navn: "Nav Bjerke",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0330",
    overordnetEnhet: "0300",
  },
  _0318: {
    navn: "Nav Nordstrand",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0318",
    overordnetEnhet: "0300",
  },
  _0334: {
    navn: "Nav Vestre Aker",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0334",
    overordnetEnhet: "0300",
  },
  _0315: {
    navn: "Nav Grünerløkka",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0315",
    overordnetEnhet: "0300",
  },
  _0300: {
    navn: "Nav Oslo",
    enhetsnummer: "0300",
    overordnetEnhet: null,
  },

  // Ost Viken
  _0106: {
    navn: "Nav Fredrikstad",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0106",
    overordnetEnhet: "0200",
  },
  _0105: {
    navn: "Nav Sarpsborg",
    type: KontorstrukturKontortype.LOKAL,
    enhetsnummer: "0105",
    overordnetEnhet: "0200",
  },
  _0200: {
    navn: "Nav Øst-Viken",
    enhetsnummer: "0200",
    overordnetEnhet: null,
  },
} as const;

export const kostnadssteder: RegionKostnadssteder[] = [
  {
    region: {
      navn: "Nav Oslo",
      enhetsnummer: "0300",
    },
    kostnadssteder: [
      {
        navn: "Nav St. Hanshaugen",
        enhetsnummer: "0313",
      },
    ],
  },
  {
    region: {
      navn: "Nav Innlandet",
      enhetsnummer: "0400",
    },
    kostnadssteder: [],
  },
];

export const kontorstruktur: Kontorstruktur[] = [
  {
    region: {
      navn: "Nav Oslo",
      enhetsnummer: "0300",
    },
    kontorer: [
      {
        navn: "Nav St. Hanshaugen",
        enhetsnummer: "0313",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Bjerke",
        enhetsnummer: "0330",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Nordstrand",
        enhetsnummer: "0318",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Vestre Aker",
        enhetsnummer: "0334",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Grünerløkka",
        enhetsnummer: "0315",
        type: KontorstrukturKontortype.LOKAL,
      },
    ],
  },
  {
    region: {
      navn: "Nav Innlandet",
      enhetsnummer: "0400",
    },
    kontorer: [
      {
        navn: "Nav Solør",
        enhetsnummer: "0425",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Kongsvinger",
        enhetsnummer: "0402",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Løten",
        enhetsnummer: "0415",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Trysil",
        enhetsnummer: "0428",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Lesja - Dovre",
        enhetsnummer: "0511",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Eidskog",
        enhetsnummer: "0420",
        type: KontorstrukturKontortype.LOKAL,
      },
    ],
  },
  {
    region: {
      navn: "Nav Øst-Viken",
      enhetsnummer: "0200",
    },
    kontorer: [
      {
        navn: "Nav Fredrikstad",
        enhetsnummer: "0106",
        type: KontorstrukturKontortype.LOKAL,
      },
      {
        navn: "Nav Sarpsborg",
        enhetsnummer: "0105",
        type: KontorstrukturKontortype.LOKAL,
      },
    ],
  },
];
