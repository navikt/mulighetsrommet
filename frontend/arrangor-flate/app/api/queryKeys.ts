import { TiltaksoversiktType, UtbetalingOversiktType } from "api-client";

export const queryKeys = {
  utbetalinger: (type: UtbetalingOversiktType) => ["utbetalinger", type] as const,
  utbetaling: (id: string) => ["utbetaling", id] as const,
  utbetalingTilsagn: (id: string) => ["utbetaling", id, "tilsagn"] as const,
  utbetalingKvittering: (id: string) => ["utbetaling", id, "kvittering"] as const,
  tilsagnRader: () => ["tilsagnRader"] as const,
  tilsagn: (id: string) => ["tilsagn", id] as const,
  tiltaksoversikt: (type: TiltaksoversiktType) => ["tiltaksoversikt", type] as const,
  opprettKravData: (orgnr: string, gjennomforingId: string) =>
    ["opprettKravData", orgnr, gjennomforingId] as const,
};
