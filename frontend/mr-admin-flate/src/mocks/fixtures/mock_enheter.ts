import {
  NavEnhet,
  NavEnhetStatus,
  Norg2Type,
} from "mulighetsrommet-api-client";

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
  {
    navn: "NAV Sogn og Fjordane",
    enhetNr: "1400",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
  },
  {
    navn: "NAV Møre og Romsdal",
    enhetNr: "1500",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
  },
  {
    navn: "NAV Innlandet",
    enhetNr: "0400",
    status: NavEnhetStatus.AKTIV,
    type: Norg2Type.FYLKE,
  },
];
