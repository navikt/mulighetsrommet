import { UtbetalingKompakt, AdminUtbetalingStatus } from "@mr/api-client-v2";

export const mockUtbetalinger: UtbetalingKompakt[] = [
  {
    beregning: {
      belop: 1000,
      periodeStart: "2025-1-1",
      periodeSlutt: "2026-6-1",
    },
    id: "",
    status: AdminUtbetalingStatus.VENTER_PA_ARRANGOR,
    delutbetalinger: [],
    createdAt: "2020-01-01",
  },
];
