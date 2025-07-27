import { NavEnhetDto, NavEnhetType, NavRegionDto } from "@api-client";

export const mockEnheter: {
  [navn: string]: NavEnhetDto;
} = {
  // Innlandet
  _0425: {
    navn: "Nav Solør",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0425",
    overordnetEnhet: "0400",
  },
  _0402: {
    navn: "Nav Kongsvinger",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0402",
    overordnetEnhet: "0400",
  },
  _0415: {
    navn: "Nav Løten",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0415",
    overordnetEnhet: "0400",
  },
  _0428: {
    navn: "Nav Trysil",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0428",
    overordnetEnhet: "0400",
  },
  _0511: {
    navn: "Nav Lesja - Dovre",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0511",
    overordnetEnhet: "0400",
  },
  _0420: {
    navn: "Nav Eidskog",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0420",
    overordnetEnhet: "0400",
  },
  _0400: {
    navn: "Nav Innlandet",
    type: NavEnhetType.FYLKE,
    enhetsnummer: "0400",
    overordnetEnhet: null,
  },

  // Oslo
  _0313: {
    navn: "Nav St. Hanshaugen",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0313",
    overordnetEnhet: "0300",
  },
  _0330: {
    navn: "Nav Bjerke",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0330",
    overordnetEnhet: "0300",
  },
  _0318: {
    navn: "Nav Nordstrand",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0318",
    overordnetEnhet: "0300",
  },
  _0334: {
    navn: "Nav Vestre Aker",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0334",
    overordnetEnhet: "0300",
  },
  _0315: {
    navn: "Nav Grünerløkka",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0315",
    overordnetEnhet: "0300",
  },
  _0300: {
    navn: "Nav Oslo",
    type: NavEnhetType.FYLKE,
    enhetsnummer: "0300",
    overordnetEnhet: null,
  },

  // Ost Viken
  _0106: {
    navn: "Nav Fredrikstad",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0106",
    overordnetEnhet: "0200",
  },
  _0105: {
    navn: "Nav Sarpsborg",
    type: NavEnhetType.LOKAL,
    enhetsnummer: "0105",
    overordnetEnhet: "0200",
  },
  _0200: {
    navn: "Nav Øst-Viken",
    type: NavEnhetType.FYLKE,
    enhetsnummer: "0200",
    overordnetEnhet: null,
  },
};

export const mockRegioner: NavRegionDto[] = [
  {
    navn: "Nav Oslo",
    enhetsnummer: "0300",
    enheter: [
      {
        navn: "Nav St. Hanshaugen",
        enhetsnummer: "0313",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Bjerke",
        enhetsnummer: "0330",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Nordstrand",
        enhetsnummer: "0318",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Vestre Aker",
        enhetsnummer: "0334",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Grünerløkka",
        enhetsnummer: "0315",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
    ],
  },
  {
    navn: "Nav Innlandet",
    enhetsnummer: "0400",
    enheter: [
      {
        navn: "Nav Solør",
        enhetsnummer: "0425",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Kongsvinger",
        enhetsnummer: "0402",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Løten",
        enhetsnummer: "0415",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Trysil",
        enhetsnummer: "0428",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Lesja - Dovre",
        enhetsnummer: "0511",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Eidskog",
        enhetsnummer: "0420",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
    ],
  },
  {
    navn: "Nav Øst-Viken",
    enhetsnummer: "0200",
    enheter: [
      {
        navn: "Nav Fredrikstad",
        enhetsnummer: "0106",
        overordnetEnhet: "0200",
        erStandardvalg: true,
      },
      {
        navn: "Nav Sarpsborg",
        enhetsnummer: "0105",
        overordnetEnhet: "0200",
        erStandardvalg: true,
      },
    ],
  },
];
