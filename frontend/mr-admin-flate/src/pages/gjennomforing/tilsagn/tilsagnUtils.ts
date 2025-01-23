import {
  TilsagnBeregningForhandsgodkjent,
  TilsagnBeregningFri,
  TilsagnDto,
} from "@mr/api-client-v2";

export function isTilsagnForhandsgodkjent(
  tilsagn: TilsagnDto,
): tilsagn is TilsagnDto & { beregning: TilsagnBeregningForhandsgodkjent } {
  return tilsagn.beregning.type === "FORHANDSGODKJENT";
}

export function isTilsagnFri(
  tilsagn: TilsagnDto,
): tilsagn is TilsagnDto & { beregning: TilsagnBeregningFri } {
  return tilsagn.beregning.type === "FRI";
}
