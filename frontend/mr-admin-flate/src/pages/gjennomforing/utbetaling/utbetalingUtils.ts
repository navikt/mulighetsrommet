import {
  UtbetalingBeregningDto,
  UtbetalingBeregningFri,
  UtbetalingBeregningPrisPerManedsverk,
  UtbetalingBeregningType,
} from "@mr/api-client-v2";

export function isBeregningFri(
  beregning: UtbetalingBeregningDto,
): beregning is UtbetalingBeregningFri {
  return beregning.type === UtbetalingBeregningType.FRI;
}

export function isBeregningPrisPerManedsverk(
  beregning: UtbetalingBeregningDto,
): beregning is UtbetalingBeregningPrisPerManedsverk {
  return beregning.type === UtbetalingBeregningType.PRIS_PER_MANEDSVERK;
}
