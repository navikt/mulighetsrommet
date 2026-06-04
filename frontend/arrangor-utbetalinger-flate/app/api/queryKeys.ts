import { ArrangorflateTilsagnFilter } from "~/hooks/useArrangorflateTilsagnRader";
import { ArrangorflateTiltakFilter } from "~/hooks/useArrangorflateTiltakRader";
import { ArrangorflateUtbetalingFilter } from "~/hooks/useArrangorflateUtbetalingRader";

export const queryKeys = {
  utbetalinger: (filter: ArrangorflateUtbetalingFilter) => ["utbetalinger", filter] as const,
  utbetaling: (id: string) => ["utbetaling", id] as const,
  utbetalingTilsagn: (id: string) => ["utbetaling", id, "tilsagn"] as const,
  utbetalingKvittering: (id: string) => ["utbetaling", id, "kvittering"] as const,
  tilsagnRader: (filter: ArrangorflateTilsagnFilter) => ["tilsagnRader", filter] as const,
  tilsagn: (id: string) => ["tilsagn", id] as const,
  tiltaksoversikt: (filter: ArrangorflateTiltakFilter) => ["tiltaksoversikt", filter] as const,
  opprettKravData: (orgnr: string, gjennomforingId: string) =>
    ["opprettKravData", orgnr, gjennomforingId] as const,
  orgnrTilganger: () => ["organisasjonsTilganger"],
};
