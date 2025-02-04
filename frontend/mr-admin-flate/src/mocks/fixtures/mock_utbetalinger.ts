import { RefusjonskravStatus, Utbetaling } from "@mr/api-client-v2";
import { mockEnheter } from "./mock_enheter";

export const mockUtbetalinger: Utbetaling[] = [
  {
    krav: {
      beregning: {
        belop: 1000,
        periodeStart: "2025-1-1",
        periodeSlutt: "2026-6-1",
      },
      id: "",
      kostnadsteder: [mockEnheter[0]],
      status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    },
    type: "UTBETALING_TIL_GODKJENNING",
    utbetalinger: [],
  },
];
