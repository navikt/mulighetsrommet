import { ArrangorflateTilsagn, RefusjonKravAft, RefusjonskravStatus } from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { v4 as uuid } from "uuid";

const mockKrav: RefusjonKravAft[] = [
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforbredene trening",
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
    deltakelser: [
      {
        id: uuid(),
        norskIdent: "01012212345",
        navn: "Per Petterson",
        manedsverk: 0.3,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 30,
          },
        ],
      },
      {
        id: uuid(),
        norskIdent: "10012212346",
        navn: "Stian Bjærvik",
        manedsverk: 1,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 100,
          },
        ],
      },
    ],
    beregning: {
      periodeStart: "01.06.2024",
      periodeSlutt: "30.06.2024",
      antallManedsverk: 17.5,
      belop: 308530,
    },
  },
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforbredene trening",
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
    deltakelser: [
      {
        id: uuid(),
        norskIdent: "01012212345",
        navn: "Per Petterson",
        manedsverk: 0.3,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 30,
          },
        ],
      },
      {
        id: uuid(),
        norskIdent: "10012212346",
        navn: "Stian Bjærvik",
        manedsverk: 1,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 100,
          },
        ],
      },
    ],
    beregning: {
      periodeStart: "01.06.2024",
      periodeSlutt: "30.06.2024",
      antallManedsverk: 1,
      belop: 18530,
    },
  },
  {
    type: "AFT",
    id: uuid(),
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforbredene trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
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
        norskIdent: "01012212345",
        navn: "Per Petterson",
        manedsverk: 0.3,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 30,
          },
        ],
      },
      {
        id: uuid(),
        norskIdent: "10012212346",
        navn: "Stian Bjærvik",
        manedsverk: 1,
        perioder: [
          {
            start: "2024-06-01",
            slutt: "2024-07-01",
            stillingsprosent: 100,
          },
        ],
      },
    ],
    beregning: {
      periodeStart: "01.06.2024",
      periodeSlutt: "30.06.2024",
      antallManedsverk: 4,
      belop: 85000,
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
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "AFT",
      periodeStart: "2024-06-01",
      periodeSlutt: "2024-12-31",
      belop: 195700,
      antallPlasser: 20,
      sats: 20205,
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
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "AFT",
      periodeStart: "2024-08-01",
      periodeSlutt: "2024-08-31",
      belop: 50000,
      antallPlasser: 2,
      sats: 20205,
    },
    gjennomforing: {
      navn: "Amo tiltak Halden",
    },
  },
];

export const arrangorflateHandlers = [
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/arrangorflate/refusjonskrav", () =>
    HttpResponse.json(mockKrav),
  ),
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/arrangorflate/refusjonskrav/:id", () =>
    HttpResponse.json(mockKrav[1]),
  ),
  http.post<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/godkjenn-refusjon",
    () => HttpResponse.json({}),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/kvittering",
    () => HttpResponse.json(undefined, { status: 501 }),
  ),
  http.get<PathParams, RefusjonKravAft[]>(
    "*/api/v1/intern/arrangorflate/refusjonskrav/:id/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/arrangorflate/tilsagn", () =>
    HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/arrangorflate/tilsagn/:id", () =>
    HttpResponse.json(mockTilsagn[0]),
  ),
];
