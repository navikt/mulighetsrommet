import {
  Arrangor,
  ArrangorflateTilsagn,
  RefusjonKravAft,
  RefusjonskravStatus,
  RelevanteForslag,
  TilsagnStatus,
} from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { v4 as uuid } from "uuid";

const mockDeltakelser = [
  {
    id: uuid(),
    person: {
      navn: "Per Petterson",
      fodselsdato: "1980-01-01",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 30,
    manedsverk: 0.3,
  },
  {
    id: uuid(),
    person: {
      navn: "Stian Bjærvik",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
  {
    id: uuid(),
    person: {
      navn: "Donald Duck",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
];

const mockKrav: RefusjonKravAft[] = [
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Moss",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    deltakelser: mockDeltakelser,
    beregning: {
      periodeStart: "2024-06-01",
      periodeSlutt: "2024-06-30",
      antallManedsverk: 17.5,
      belop: 308530,
      digest: "ac6b2cdcbfc885e64121cf4e0ebee5dd",
    },
  },
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    deltakelser: mockDeltakelser,
    beregning: {
      periodeStart: "2024-06-01",
      periodeSlutt: "2024-06-30",
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
    },
  },
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    deltakelser: [
      {
        id: uuid(),
        person: {
          navn: "Per Petterson",
          fodselsdato: "1980-01-01",
          fodselsaar: 1980,
        },
        startDato: "2024-06-01",
        forstePeriodeStartDato: "2024-06-01",
        sistePeriodeSluttDato: "2024-06-30",
        sistePeriodeDeltakelsesprosent: 30,
        manedsverk: 0.3,
      },
      {
        id: uuid(),
        person: {
          navn: "Stian Bjærvik",
        },
        startDato: "2024-06-01",
        forstePeriodeStartDato: "2024-06-01",
        sistePeriodeSluttDato: "2024-06-30",
        sistePeriodeDeltakelsesprosent: 100,
        manedsverk: 1,
      },
    ],
    beregning: {
      periodeStart: "2024-06-01",
      periodeSlutt: "2024-06-30",
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
    },
  },
];

const mockTilsagn: ArrangorflateTilsagn[] = [
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periodeStart: "2024-06-01",
    periodeSlutt: "2024-12-31",
    status: TilsagnStatus.GODKJENT,
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periodeStart: "2024-06-01",
        periodeSlutt: "2024-12-31",
        antallPlasser: 20,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 195700,
      },
    },
    gjennomforing: {
      navn: "Amo tiltak Halden",
    },
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periodeStart: "2024-08-01",
    periodeSlutt: "2024-08-31",
    status: TilsagnStatus.GODKJENT,
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periodeStart: "2024-08-01",
        periodeSlutt: "2024-08-31",
        antallPlasser: 2,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 50000,
      },
    },
    gjennomforing: {
      navn: "Amo tiltak Halden",
    },
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periodeStart: "2024-08-01",
    periodeSlutt: "2024-08-31",
    status: TilsagnStatus.GODKJENT,
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periodeStart: "2024-08-01",
        periodeSlutt: "2024-08-31",
        antallPlasser: 2,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 50000,
      },
    },
    gjennomforing: {
      navn: "Amo tiltak Halden",
    },
  },
];

const arrangorer: Arrangor[] = [
  {
    id: uuid(),
    organisasjonsnummer: "123456789",
    navn: "Fretex",
    overordnetEnhet: null,
  },
];

const mockRelevanteForslag: RelevanteForslag[] = [
  {
    deltakerId: mockDeltakelser[0].id,
    antallRelevanteForslag: 1,
  },
  {
    deltakerId: mockDeltakelser[1].id,
    antallRelevanteForslag: 0,
  },
];

export const arrangorflateHandlers = [
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/refusjonskrav",
    () => HttpResponse.json(mockKrav),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(mockKrav.find((k) => k.id === id));
    },
  ),
  http.post<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/godkjenn-refusjon",
    () => HttpResponse.json({}),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/:orgnr/refusjonskrav/:id/kvittering",
    () => HttpResponse.json(undefined, { status: 501 }),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/relevante-forslag",
    () => HttpResponse.json(mockRelevanteForslag),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/:orgnr/tilsagn/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(mockTilsagn.find((k) => k.id === id));
    },
  ),
  http.get<PathParams, Arrangor[]>("*/api/v1/intern/arrangorflate/tilgang-arrangor", () =>
    HttpResponse.json(arrangorer),
  ),
];
