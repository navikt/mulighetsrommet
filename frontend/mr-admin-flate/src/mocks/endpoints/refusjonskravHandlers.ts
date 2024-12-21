import { RefusjonKravKompakt, RefusjonskravStatus } from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { mockTiltaksgjennomforinger } from "../fixtures/mock_tiltaksgjennomforinger";

const mockRefusjonskravKompakt: RefusjonKravKompakt[] = [
  {
    arrangor: {
      id: "d9d4db51-3564-4493-b897-4fc38dc48965",
      organisasjonsnummer: "992943084",
      navn: "FRETEX AS AVD OSLO",
      slettet: false,
    },
    beregning: {
      periodeStart: "2024-08-01",
      periodeSlutt: "2024-08-31",
      belop: 123000,
    },
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    gjennomforing: {
      navn: mockTiltaksgjennomforinger[0].navn,
      id: mockTiltaksgjennomforinger[0].id,
    },
    tiltakstype: mockTiltaksgjennomforinger[0].tiltakstype,
    fristForGodkjenning: "2024-10-31",
    status: RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
  },
  {
    arrangor: {
      id: "d9d4db51-3564-4493-b897-4fc38dc48965",
      organisasjonsnummer: "992943084",
      navn: "FRETEX AS AVD OSLO",
      slettet: false,
    },
    beregning: {
      periodeStart: "2024-09-01",
      periodeSlutt: "2024-09-30",
      belop: 89000,
    },
    id: "73e393b0-1b7c-4c68-9a42-b541b2f114b8",
    gjennomforing: {
      navn: mockTiltaksgjennomforinger[0].navn,
      id: mockTiltaksgjennomforinger[0].id,
    },
    tiltakstype: mockTiltaksgjennomforinger[0].tiltakstype,
    fristForGodkjenning: "2024-11-30",
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
  },
  {
    arrangor: {
      id: "d9d4db51-3564-4493-b897-4fc38dc48965",
      organisasjonsnummer: "992943084",
      navn: "FRETEX AS AVD OSLO",
      slettet: false,
    },
    beregning: {
      periodeStart: "2024-10-01",
      periodeSlutt: "2024-10-31",
      belop: 89000,
    },
    id: "793e393b0-1b7c-4c68-9a42-b541b2f114b8",
    gjennomforing: {
      navn: mockTiltaksgjennomforinger[0].navn,
      id: mockTiltaksgjennomforinger[0].id,
    },
    tiltakstype: mockTiltaksgjennomforinger[0].tiltakstype,
    fristForGodkjenning: "2024-12-31",
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
  },
];

export const refusjonskravHandlers = [
  http.get<PathParams, any, RefusjonKravKompakt[]>(
    "*/api/v1/intern/tiltaksgjennomforinger/:tiltaksgjennomforingId/refusjonskrav",
    async () => {
      return HttpResponse.json(mockRefusjonskravKompakt);
    },
  ),
  http.get<PathParams, any, RefusjonKravKompakt>(
    "*/api/v1/intern/refusjonskrav/:refusjonskravId",
    async ({ params }) => {
      const { refusjonskravId } = params;

      const krav = mockRefusjonskravKompakt.find((t) => t.id === refusjonskravId);
      return HttpResponse.json(krav);
    },
  ),
];
