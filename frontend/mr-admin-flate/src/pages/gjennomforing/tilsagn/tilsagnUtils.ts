import {
  TilsagnBeregning,
  TilsagnBeregningFri,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningType,
} from "@mr/api-client-v2";

export function isBeregningFri(beregning: TilsagnBeregning): beregning is TilsagnBeregningFri {
  return beregning.type === TilsagnBeregningType.FRI;
}

export function isBeregningPrisPerManedsverk(
  beregning: TilsagnBeregning,
): beregning is TilsagnBeregningPrisPerManedsverk {
  return beregning.type === TilsagnBeregningType.PRIS_PER_MANEDSVERK;
}
