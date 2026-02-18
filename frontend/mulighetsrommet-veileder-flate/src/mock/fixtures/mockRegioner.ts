import { Kontorstruktur, KontorstrukturKontortype } from "@api-client";

export const mockRegioner: Kontorstruktur[] = [
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
