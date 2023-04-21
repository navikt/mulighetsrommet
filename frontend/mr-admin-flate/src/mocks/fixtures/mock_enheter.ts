import {
  NavEnhet,
  NavEnhetStatus,
  Norg2Type,
} from "mulighetsrommet-api-client";

export const mockEnheter: NavEnhet[] = [
  {
    navn: "NAV Øst-Viken",
    enhetNr: "0200",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
  },
  {
    navn: "NAV Nordre Follo",
    enhetNr: "0213",
    status: NavEnhetStatus.AKTIV,
    overordnetEnhet: "0200",
  },
  {
    navn: "NAV Hvaler",
    enhetNr: "0111",
    status: NavEnhetStatus.AKTIV,
    overordnetEnhet: "0200",
  },
  {
    navn: "NAV Sarpsborg",
    enhetNr: "0105",
    status: NavEnhetStatus.AKTIV,
    overordnetEnhet: "0200",
  },
  {
    navn: "NAV Hillevåg og Hinna",
    enhetNr: "1164",
    status: NavEnhetStatus.AKTIV,
    overordnetEnhet: "1100",
  },
  {
    navn: "NAV Nordre Aker",
    enhetNr: "0331",
    status: NavEnhetStatus.AKTIV,
    overordnetEnhet: "0300",
  },
];
