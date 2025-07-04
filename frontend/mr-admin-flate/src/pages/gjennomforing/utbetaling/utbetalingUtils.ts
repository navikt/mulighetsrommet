import {
  UtbetalingBeregning,
  UtbetalingBeregningFri,
  UtbetalingBeregningPrisPerManedsverk,
  UtbetalingBeregningType,
} from "@mr/api-client-v2";

export function isBeregningFri(
  beregning: UtbetalingBeregning,
): beregning is UtbetalingBeregningFri {
  return beregning.type === UtbetalingBeregningType.FRI;
}

export function isBeregningPrisPerManedsverk(
  beregning: UtbetalingBeregning,
): beregning is UtbetalingBeregningPrisPerManedsverk {
  return beregning.type === UtbetalingBeregningType.PRIS_PER_MANEDSVERK;
}
