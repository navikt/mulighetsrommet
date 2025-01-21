import {
  NavEnhetStatus,
  NavEnhetType,
  RefusjonKravKompakt,
  RefusjonskravStatus,
} from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";

const mockRefusjonskravKompakt: RefusjonKravKompakt[] = [
  {
    beregning: {
      periodeStart: "2024-08-01",
      periodeSlutt: "2024-08-31",
      belop: 123000,
    },
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    status: RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
    kostnadsteder: [],
  },
  {
    beregning: {
      periodeStart: "2024-09-01",
      periodeSlutt: "2024-09-30",
      belop: 89000,
    },
    id: "73e393b0-1b7c-4c68-9a42-b541b2f114b8",
    kostnadsteder: [],
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
  },
  {
    kostnadsteder: [
      {
        enhetsnummer: "0234",
        navn: "Nav Oslo",
        status: NavEnhetStatus.AKTIV,
        overordnetEnhet: null,
        type: NavEnhetType.FYLKE,
      },
    ],
    beregning: {
      periodeStart: "2024-10-01",
      periodeSlutt: "2024-10-31",
      belop: 89000,
    },
    id: "793e393b0-1b7c-4c68-9a42-b541b2f114b8",
    status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
  },
];

export const refusjonskravHandlers = [
  http.get<PathParams, any, RefusjonKravKompakt[]>(
    "*/api/v1/intern/gjennomforinger/:gjennomforingId/refusjonskrav",
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
