import {Avtalestatus, Avtaletype, PaginertAvtale} from "mulighetsrommet-api-client";

export const mockAvtaler: PaginertAvtale = {
  pagination: {
    totalCount: 104,
    currentPage: 1,
    pageSize: 50,
  },
  data: [
    {
      id: "1",
      navn: "Avtale om opplæring av blinde superhelter",
      leverandorOrganisasjonsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-01-11",
      sluttDato: "2024-01-11",
      avtalenummer: "2023#1",
      avtalestatus: Avtalestatus.AKTIV,
      avtaletype: Avtaletype.RAMMEAVTALE,
      prisbetingelser: null,
      tiltakstype: {
        navn: "Opplæring",
        arenaKode: "INDOPPFAG",
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d"
      }
    },
    {
      id: "2",
      navn: "Avtale om oppfølging av vaklevorne gjess",
      leverandorOrganisasjonsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-05-17",
      sluttDato: "2024-12-24",
      avtalenummer: "2023#2",
      avtalestatus: Avtalestatus.AKTIV,
      avtaletype: Avtaletype.RAMMEAVTALE,
      prisbetingelser: null,
      tiltakstype: {
        navn: "Opplæring",
        arenaKode: "INDOPPFAG",
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d"
      }
    },
    {
      id: "3",
      navn: "Avtale om opplæring av brusdistributører",
      leverandorOrganisasjonsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-01-09",
      sluttDato: "2024-01-15",
      avtalenummer: "2023#3",
      avtalestatus: Avtalestatus.AKTIV,
      avtaletype: Avtaletype.RAMMEAVTALE,
      prisbetingelser: null,
      tiltakstype: {
        navn: "Opplæring",
        arenaKode: "INDOPPFAG",
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d"
      }
    },
  ],
};
