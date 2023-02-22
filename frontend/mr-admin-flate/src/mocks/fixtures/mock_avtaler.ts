import {
  Avtalestatus,
  Avtaletype,
  PaginertAvtale,
} from "mulighetsrommet-api-client";

export const mockAvtaler: PaginertAvtale = {
  pagination: {
    totalCount: 82,
    currentPage: 1,
    pageSize: 15,
  },
  data: [
    {
      id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos ÅMLI KOMMUNE SAMFUNNSAVDELINGA",
      avtalenummer: "2021#10579",
      leverandor: {
        organisasjonsnummer: "864965962",
        navn: "ÅMLI KOMMUNE",
      },

      startDato: "2021-08-02",
      sluttDato: "2026-08-01",
      navEnhet: {
        enhetsnummer: "1087",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "6374b285-989d-4f78-a59e-29481b64ba92",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Åna Fengsel",
      avtalenummer: "2020#4929",
      leverandor: {
        organisasjonsnummer: "911830868",
        navn: "KRIMINALOMSORGSDIREKTORATET",
      },

      startDato: "2020-07-01",
      sluttDato: "2024-06-30",
      navEnhet: {
        enhetsnummer: "1187",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "1eea449a-2629-4abc-b151-76ebbd028401",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Arbeids- og sosialdepartementet",
      avtalenummer: "2020#4993",
      leverandor: {
        organisasjonsnummer: "983887457",
        navn: "ARBEIDS- OG INKLUDERINGSDEPARTEMENTET",
      },

      startDato: "2020-07-01",
      sluttDato: "2023-06-30",
      navEnhet: {
        enhetsnummer: "0587",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "8a4a4dee-98c7-4a07-bc0c-f677a46c406f",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Arendal fengsel",
      avtalenummer: "2020#6480",
      leverandor: {
        organisasjonsnummer: "911830868",
        navn: "KRIMINALOMSORGSDIREKTORATET",
      },

      startDato: "2020-07-01",
      sluttDato: "2024-06-30",
      navEnhet: {
        enhetsnummer: "0800",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "6b33bdbf-621d-4e4a-aa35-3dea18f02327",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos ASKØY KOMMUNE BARNEHAGEADMINISTRASJ",
      avtalenummer: "2021#13076",
      leverandor: {
        organisasjonsnummer: "911615649",
        navn: "ASKØY KOMMUNE BARNEHAGE",
      },

      startDato: "2021-11-01",
      sluttDato: "2023-10-31",
      navEnhet: {
        enhetsnummer: "1287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "bd429f41-0222-4374-82af-ccf932e2348d",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos ASKØY KOMMUNE KOMM TEKNISK AVD",
      avtalenummer: "2021#17845",
      leverandor: {
        organisasjonsnummer: "964338442",
        navn: "ASKØY KOMMUNE",
      },

      startDato: "2021-11-01",
      sluttDato: "2024-06-30",
      navEnhet: {
        enhetsnummer: "1287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "e26c53cd-cafd-4a57-b7e0-c774cf33ea0d",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos AUST-AGDER SIVILFORSVARSDISTRIKT",
      avtalenummer: "2019#6552",
      leverandor: {
        organisasjonsnummer: "974760983",
        navn: "DIREKTORATET FOR SAMFUNNSSIKKERHET OG BEREDSKAP (DSB)",
      },

      startDato: "2019-08-20",
      sluttDato: "2023-08-19",
      navEnhet: {
        enhetsnummer: "1087",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "6267aed1-1d7a-419a-83aa-1c42488b6bf1",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Bærum kommune Vei Og Trafikk",
      avtalenummer: "2021#20070",
      leverandor: {
        organisasjonsnummer: "995448394",
        navn: "BÆRUM KOMMUNE MILJØTEKNISK",
      },

      startDato: "2022-01-01",
      sluttDato: "2025-05-09",
      navEnhet: {
        enhetsnummer: "1187",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "dd85e265-30ad-4474-b3e5-7f5670b978c3",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos BUSINESS REGION KRISTIANSAND",
      avtalenummer: "2021#14456",
      leverandor: {
        organisasjonsnummer: "918781390",
        navn: "KRISTIANSAND KOMMUNE SAMHANDLING OG INNOVASJON",
      },

      startDato: "2021-10-01",
      sluttDato: "2024-12-31",
      navEnhet: {
        enhetsnummer: "1087",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "bc5fb428-e712-460c-9153-9e2f6caadf82",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos BYBANEN - BYBANEKONTORET",
      avtalenummer: "2020#6620",
      leverandor: {
        organisasjonsnummer: "976821580",
        navn: "BERGEN KOMMUNE BYRÅDSAVDELING FOR KLIMA, MILJØ OG BYUTVIKLING",
      },
      startDato: "2020-07-01",
      sluttDato: "2023-06-30",
      navEnhet: {
        enhetsnummer: "1287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "f80817a4-4602-46e4-ae62-46ad28b023be",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Bydelsadm Bydel Grünerløkka",
      avtalenummer: "2019#2191",
      leverandor: {
        organisasjonsnummer: "870534612",
        navn: "OSLO KOMMUNE BYDEL 2 GRUNERLØKKA",
      },

      startDato: "2019-07-01",
      sluttDato: "2023-06-30",
      navEnhet: {
        enhetsnummer: "1500",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "03d8e390-6c4d-4d8c-b42d-d9f39406086b",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos DRAMMENSREGIONENS BRANNVESEN IKS",
      avtalenummer: "2020#12202",
      leverandor: {
        organisasjonsnummer: "984054408",
        navn: "DRAMMENSREGIONENS BRANNVESEN IKS",
      },

      startDato: "2020-10-01",
      sluttDato: "2023-09-30",
      navEnhet: {
        enhetsnummer: "0687",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "21ab610d-5ed9-4d08-bdba-0650e2a56601",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos HÅ KOMMUNE SENTRALADMINISTRASJON",
      avtalenummer: "2021#18282",
      leverandor: {
        organisasjonsnummer: "964969590",
        navn: "HÅ KOMMUNE",
      },

      startDato: "2021-09-21",
      sluttDato: "2024-09-20",
      navEnhet: {
        enhetsnummer: "0287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "f671039e-796b-456f-a4b3-c95172e2142c",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Hardanger likningskontor",
      avtalenummer: "2021#20250",
      leverandor: {
        organisasjonsnummer: "974761076",
        navn: "SKATTEETATEN",
      },

      startDato: "2022-01-01",
      sluttDato: "2024-12-31",
      navEnhet: {
        enhetsnummer: "1287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
    {
      id: "8179667d-4cdb-4307-ac34-d459bda709a3",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "Oppfølging",
        arenaKode: "INDOPPFAG",
      },
      navn: "Avtale hos Hardanger likningskontor",
      avtalenummer: "2021#20172",
      leverandor: {
        organisasjonsnummer: "974761076",
        navn: "SKATTEETATEN",
      },

      startDato: "2022-01-01",
      sluttDato: "2024-12-31",
      navEnhet: {
        enhetsnummer: "1287",
        navn: "NAV Våler",
      },
      avtaletype: Avtaletype.RAMMEAVTALE,
      avtalestatus: Avtalestatus.AKTIV,
      prisbetingelser: "Maskert prisbetingelser",
    },
  ],
};
