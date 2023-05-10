import {
  PaginertTiltaksgjennomforing,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";

export const mockTiltaksgjennomforinger: PaginertTiltaksgjennomforing = {
  pagination: {
    totalCount: 18,
    currentPage: 1,
    pageSize: 50,
  },
  data: [
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
      navn: "Yrkesnorsk med praksis",
      tiltaksnummer: "123456",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Valp AS",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "2990",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee362",
      navn: "Spillbasert kvalifisering",
      tiltaksnummer: "123456",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "2990",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.AVLYST,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
      navn: "Midlertidig lønnstilskudd",
      tiltaksnummer: "654434",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Solo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "2990",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee364",
      navn: "AFT - Unikom",
      tiltaksnummer: "768672",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Valp AS",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee365",
      navn: "Varig lønnstilskudd",
      tiltaksnummer: "65645",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Valp AS",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee366",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "32557",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Utvikler AS",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.AVBRUTT,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee367",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "98643",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee368",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "575685",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.AVLYST,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee369",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "54353",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee310",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "23213",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee311",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "76575",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee312",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "23123",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Lady Grey",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee313",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "686585",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee314",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "43242",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee315",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "4367",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee316",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "7685",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee317",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "5435356",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "SoloPolo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee318",
      navn: "Arbeidsrettet rehabilitering - Trondheim",
      tiltaksnummer: "987643",
      virksomhetsnummer: "1000",
      virksomhetsnavn: "Solo",
      tiltakstype: {
        id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
        navn: "TILTAKSTYPENAVN",
        arenaKode: "ABIST",
      },
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      arenaAnsvarligEnhet: "1190",
      navEnheter: [],
      status: TiltaksgjennomforingStatus.APENT_FOR_INNSOK,
    },
  ],
};
