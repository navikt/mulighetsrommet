import { TilsagnBeregning, TilsagnBeregningAft, TilsagnBeregningFri } from "@mr/api-client";

export function isAftBeregning(beregning: TilsagnBeregning): beregning is TilsagnBeregningAft {
  return beregning.type === "AFT";
}

export function isFriBeregning(beregning: TilsagnBeregning): beregning is TilsagnBeregningFri {
  return beregning.type === "FRI";
}
