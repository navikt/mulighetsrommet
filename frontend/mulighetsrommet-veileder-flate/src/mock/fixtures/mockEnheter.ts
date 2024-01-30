import { NavEnhet, NavEnhetStatus, NavEnhetType, NavRegion } from "mulighetsrommet-api-client";

export const mockEnheter: {
  [navn: string]: NavEnhet;
} = {
  // Innlandet
  _0425: {
    navn: "NAV Solør",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0425",
    overordnetEnhet: "0400",
  },
  _0402: {
    navn: "NAV Kongsvinger",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0402",
    overordnetEnhet: "0400",
  },
  _0415: {
    navn: "NAV Løten",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0415",
    overordnetEnhet: "0400",
  },
  _0428: {
    navn: "NAV Trysil",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0428",
    overordnetEnhet: "0400",
  },
  _0511: {
    navn: "NAV Lesja - Dovre",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0511",
    overordnetEnhet: "0400",
  },
  _0420: {
    navn: "NAV Eidskog",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0420",
    overordnetEnhet: "0400",
  },
  _0400: {
    navn: "NAV Innlandet",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0400",
    overordnetEnhet: null,
  },

  // Oslo
  _0313: {
    navn: "NAV St. Hanshaugen",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0313",
    overordnetEnhet: "0300",
  },
  _0330: {
    navn: "NAV Bjerke",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0330",
    overordnetEnhet: "0300",
  },
  _0318: {
    navn: "NAV Nordstrand",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0318",
    overordnetEnhet: "0300",
  },
  _0334: {
    navn: "NAV Vestre Aker",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0334",
    overordnetEnhet: "0300",
  },
  _0315: {
    navn: "NAV Grünerløkka",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0315",
    overordnetEnhet: "0300",
  },
  _0300: {
    navn: "NAV Oslo",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0300",
    overordnetEnhet: null,
  },

  // Ost Viken
  _0106: {
    navn: "NAV Fredrikstad",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0106",
    overordnetEnhet: "0200",
  },
  _0105: {
    navn: "NAV Sarpsborg",
    type: NavEnhetType.LOKAL,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0105",
    overordnetEnhet: "0200",
  },
  _0200: {
    navn: "NAV Øst-Viken",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0200",
    overordnetEnhet: null,
  },
};

export const mockRegioner: NavRegion[] = [
  {
    navn: "NAV Oslo",
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
    navn: "NAV Innlandet",
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
    navn: "NAV Øst-Viken",
    type: NavEnhetType.FYLKE,
    status: NavEnhetStatus.AKTIV,
    enhetsnummer: "0200",
    enheter: [mockEnheter._0106, mockEnheter._0105],
  },
];
