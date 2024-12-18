import { TilsagnDto, TilsagnBeregningAFT, TilsagnBeregningFri } from "@mr/api-client";

export function isAftBeregning(
  beregning: TilsagnDto["beregning"],
): beregning is TilsagnBeregningAFT {
  return beregning.type === "AFT";
}

export function isFriBeregning(
  beregning: TilsagnDto["beregning"],
): beregning is TilsagnBeregningFri {
  return beregning.type === "FRI";
}
