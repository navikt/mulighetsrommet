import { NavEnhet, NavEnhetStatus, Norg2Type } from "mulighetsrommet-api-client";

export const mockEnheter: { [navn: string]: NavEnhet } = {
  // Innlandet
  _0425: { navn: "NAV Solør", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0425", overordnetEnhet: "0400" },
  _0402: { navn: "NAV Kongsvinger", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0402", overordnetEnhet: "0400" },
  _0415: { navn: "NAV Løten", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0415", overordnetEnhet: "0400" },
  _0428: { navn: "NAV Trysil", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0428", overordnetEnhet: "0400" },
  _0511: { navn: "NAV Lesja - Dovre", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0511", overordnetEnhet: "0400" },
  _0420: { navn: "NAV Eidskog", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0420", overordnetEnhet: "0400" },
  _0400: { navn: "NAV Innlandet", type: Norg2Type.FYLKE, status: NavEnhetStatus.AKTIV, enhetsnummer: "0400", overordnetEnhet: null },

  // Oslo
  _0313: { navn: "NAV St. Hanshaugen", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0313", overordnetEnhet: "0300" },
  _0330: { navn: "NAV Bjerke", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0330", overordnetEnhet: "0300" },
  _0318: { navn: "NAV Nordstrand", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0318", overordnetEnhet: "0300" },
  _0334: { navn: "NAV Vestre Aker", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0334", overordnetEnhet: "0300" },
  _0315: { navn: "NAV Grünerløkka", type: Norg2Type.LOKAL, status: NavEnhetStatus.AKTIV, enhetsnummer: "0315", overordnetEnhet: "0300" },
  _0300: { navn: "NAV Oslo", type: Norg2Type.FYLKE, status: NavEnhetStatus.AKTIV, enhetsnummer: "0300", overordnetEnhet: null },
};
