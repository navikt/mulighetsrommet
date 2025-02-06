import {
  Arrangor,
  ArrangorflateTilsagn,
  ArrFlateRefusjonKrav,
  RefusjonskravStatus,
  RelevanteForslag,
  TilsagnStatus,
  TilsagnType,
} from "@mr/api-client-v2";
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

const mockKrav: ArrFlateRefusjonKrav[] = [
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
    periodeStart: "2024-06-01",
    periodeSlutt: "2024-06-30",
    beregning: {
      antallManedsverk: 17.5,
      belop: 308530,
      digest: "ac6b2cdcbfc885e64121cf4e0ebee5dd",
      deltakelser: mockDeltakelser,
      type: "FORHANDSGODKJENT",
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
    periodeStart: "2024-06-01",
    periodeSlutt: "2024-06-30",
    beregning: {
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
      deltakelser: mockDeltakelser,
      type: "FORHANDSGODKJENT",
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
    periodeStart: "2024-06-01",
    periodeSlutt: "2024-06-30",
    beregning: {
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
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
      type: "FORHANDSGODKJENT",
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
    status: { status: TilsagnStatus.GODKJENT },
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
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periodeStart: "2024-08-01",
    periodeSlutt: "2024-08-31",
    status: { status: TilsagnStatus.GODKJENT },
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
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periodeStart: "2024-08-01",
    periodeSlutt: "2024-08-31",
    status: { status: TilsagnStatus.GODKJENT },
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
    type: TilsagnType.EKSTRATILSAGN,
  },
];

const arrangorer: Arrangor[] = [
  {
    id: uuid(),
    organisasjonsnummer: "123456789",
    organisasjonsform: "AS",
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
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/refusjonskrav",
    () => HttpResponse.json(mockKrav),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(mockKrav.find((k) => k.id === id));
    },
  ),
  http.post<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/godkjenn-refusjon",
    () => HttpResponse.json({}),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/:orgnr/refusjonskrav/:id/kvittering",
    () => HttpResponse.json(undefined, { status: 501 }),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/relevante-forslag",
    () => HttpResponse.json(mockRelevanteForslag),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, ArrFlateRefusjonKrav[]>(
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
