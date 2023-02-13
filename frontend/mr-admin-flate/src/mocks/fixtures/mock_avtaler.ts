import { PaginertAvtale } from "mulighetsrommet-api-client";

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
      virksomhetsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-01-11",
      sluttDato: "2024-01-11",
      tiltakstypeID: "afb69ca8-ddff-45be-9fd0-8f968519468d",
    },
    {
      id: "2",
      navn: "Avtale om oppfølging av vaklevorne gjess",
      virksomhetsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-05-17",
      sluttDato: "2024-12-24",
      tiltakstypeID: "afb69ca8-ddff-45be-9fd0-8f968519468d",
    },
    {
      id: "3",
      navn: "Avtale om opplæring av brusdistributører",
      virksomhetsnummer: "Joblearn AS 981428781",
      enhet: "1801 Oslo",
      startDato: "2023-01-09",
      sluttDato: "2024-01-15",
      tiltakstypeID: "afb69ca8-ddff-45be-9fd0-8f968519468d",
    },
  ],
};
