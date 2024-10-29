import { NavEnhet, NavEnhetStatus, NavEnhetType, NavRegion } from "@mr/api-client";

export const mockEnheter: {
  [navn: string]: NavEnhet;
} = {
  // Innlandet
  _0425: {
    navn: "Nav Solør",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0425",
    overordnetEnhet: "0400",
  },
  _0402: {
    navn: "Nav Kongsvinger",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0402",
    overordnetEnhet: "0400",
  },
  _0415: {
    navn: "Nav Løten",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0415",
    overordnetEnhet: "0400",
  },
  _0428: {
    navn: "Nav Trysil",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0428",
    overordnetEnhet: "0400",
  },
  _0511: {
    navn: "Nav Lesja - Dovre",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0511",
    overordnetEnhet: "0400",
  },
  _0420: {
    navn: "Nav Eidskog",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0420",
    overordnetEnhet: "0400",
  },
  _0400: {
    navn: "Nav Innlandet",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0400",
    overordnetEnhet: null,
  },

  // Oslo
  _0313: {
    navn: "Nav St. Hanshaugen",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0313",
    overordnetEnhet: "0300",
  },
  _0330: {
    navn: "Nav Bjerke",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0330",
    overordnetEnhet: "0300",
  },
  _0318: {
    navn: "Nav Nordstrand",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0318",
    overordnetEnhet: "0300",
  },
  _0334: {
    navn: "Nav Vestre Aker",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0334",
    overordnetEnhet: "0300",
  },
  _0315: {
    navn: "Nav Grünerløkka",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0315",
    overordnetEnhet: "0300",
  },
  _0300: {
    navn: "Nav Oslo",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0300",
    overordnetEnhet: null,
  },

  // Ost Viken
  _0106: {
    navn: "Nav Fredrikstad",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0106",
    overordnetEnhet: "0200",
  },
  _0105: {
    navn: "Nav Sarpsborg",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0105",
    overordnetEnhet: "0200",
  },
  _0200: {
    navn: "Nav Øst-Viken",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0200",
    overordnetEnhet: null,
  },
};

export const mockRegioner: NavRegion[] = [
  {
    navn: "Nav Oslo",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0300",
    enheter: [
      mockEnheter._0313,
      mockEnheter._0330,
      mockEnheter._0318,
      mockEnheter._0334,
      mockEnheter._0315,
    ],
  },
  {
    navn: "Nav Innlandet",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0400",
    enheter: [
      mockEnheter._0425,
      mockEnheter._0402,
      mockEnheter._0415,
      mockEnheter._0428,
      mockEnheter._0511,
      mockEnheter._0420,
    ],
  },
  {
    navn: "Nav Øst-Viken",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0200",
    enheter: [mockEnheter._0106, mockEnheter._0105],
  },
];
