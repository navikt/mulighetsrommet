import { TilsagnDto, TilsagnBeregningAFT } from "@mr/api-client";

export function isAftBeregning(
  beregning: TilsagnDto["beregning"],
): beregning is TilsagnBeregningAFT {
  return beregning.type === "AFT";
}
