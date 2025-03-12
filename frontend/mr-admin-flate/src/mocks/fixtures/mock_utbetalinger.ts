import { UtbetalingKompakt, AdminUtbetalingStatus } from "@mr/api-client-v2";

export const mockUtbetalinger: UtbetalingKompakt[] = [
  {
    id: "",
    periode: {
      start: "2025-01-01",
      slutt: "2026-06-01",
    },
    status: AdminUtbetalingStatus.VENTER_PA_ARRANGOR,
    createdAt: "2020-01-01",
    beregning: {
      belop: 1000,
    },
  },
];
