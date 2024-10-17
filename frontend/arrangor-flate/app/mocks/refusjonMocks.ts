import { RefusjonKravAft, RefusjonskravStatus } from "@mr/api-client";
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
    deltakelser: [],
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
    deltakelser: [],
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
    deltakelser: [],
    beregning: {
      periodeStart: "01.06.2024",
      periodeSlutt: "30.06.2024",
      antallManedsverk: 4,
      belop: 85000,
    },
  },
];

export const refusjonHandlers = [
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/refusjon/:orgnr/krav", () =>
    HttpResponse.json(mockKrav),
  ),
  http.get<PathParams, RefusjonKravAft[]>("*/api/v1/intern/refusjon/krav/:id", () =>
    HttpResponse.json(mockKrav[1]),
  ),
];
