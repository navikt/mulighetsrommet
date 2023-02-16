import { Enhet } from "mulighetsrommet-api-client";

export const mockEnheter: Enhet[] = [
  {
    enhetId: 100000027,
    navn: "NAV Bærum",
    enhetNr: "0219",
    status: Enhet.status.AKTIV,
  },
  {
    enhetId: 100000238,
    navn: "NAV Hillevåg og Hinna",
    enhetNr: "1164",
    status: Enhet.status.AKTIV,
  },
  {
    enhetId: 100000005,
    navn: "NAV Hvaler",
    enhetNr: "0111",
    status: Enhet.status.AKTIV,
  },
  {
    enhetId: 100000058,
    navn: "NAV Nordre Aker",
    enhetNr: "0331",
    status: Enhet.status.AKTIV,
  },
];
