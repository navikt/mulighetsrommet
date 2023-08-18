import {
  NavEnhet,
  NavEnhetStatus,
  Norg2Type,
} from "mulighetsrommet-api-client";

export const mockEnheter: NavEnhet[] = [
  {
    navn: "NAV Bærum",
    enhetsnummer: "0219",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.LOKAL,
    overordnetEnhet: "1400",
  },
  {
    navn: "NAV Hillevåg og Hinna",
    enhetsnummer: "1164",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.LOKAL,
    overordnetEnhet: "0400",
  },
  {
    navn: "NAV Hvaler",
    enhetsnummer: "0111",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.LOKAL,
    overordnetEnhet: "1500",
  },
  {
    navn: "NAV Nordre Aker",
    enhetsnummer: "0331",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.LOKAL,
    overordnetEnhet: "1500",
  },
  {
    navn: "NAV Østre Aker",
    enhetsnummer: "0332",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.LOKAL,
    overordnetEnhet: "0400",
  },
  {
    navn: "NAV Sogn og Fjordane",
    enhetsnummer: "1400",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
    overordnetEnhet: null,
  },
  {
    navn: "NAV Møre og Romsdal",
    enhetsnummer: "1500",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
    overordnetEnhet: null,
  },
  {
    navn: "NAV Innlandet",
    enhetsnummer: "0400",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
    overordnetEnhet: null,
  },
];
