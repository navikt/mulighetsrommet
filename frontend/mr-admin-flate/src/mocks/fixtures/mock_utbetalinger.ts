import { RefusjonskravStatus, Utbetaling } from "@mr/api-client-v2";

export const mockUtbetalinger: Utbetaling[] = [
  {
    krav: {
      beregning: {
        belop: 1000,
        periodeStart: "2025-1-1",
        periodeSlutt: "2026-6-1",
      },
      id: "",
      status: RefusjonskravStatus.KLAR_FOR_GODKJENNING,
    },
    type: "UTBETALING_TIL_GODKJENNING",
    utbetalinger: [],
  },
];
