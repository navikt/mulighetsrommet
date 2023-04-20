import { NavEnhet, NavEnhetStatus } from "mulighetsrommet-api-client";

export const mockEnheter: NavEnhet[] = [
  {
    navn: "NAV Bærum",
    enhetNr: "0219",
    status: NavEnhetStatus.AKTIV,
  },
  {
    navn: "NAV Hillevåg og Hinna",
    enhetNr: "1164",
    status: NavEnhetStatus.AKTIV,
  },
  {
    navn: "NAV Hvaler",
    enhetNr: "0111",
    status: NavEnhetStatus.AKTIV,
  },
  {
    navn: "NAV Nordre Aker",
    enhetNr: "0331",
    status: NavEnhetStatus.AKTIV,
  },
];
