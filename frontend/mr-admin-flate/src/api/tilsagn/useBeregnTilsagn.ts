import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  BeregnTilsagnService,
  TilsagnBeregningInput,
  TilsagnBeregningOutput,
} from "@mr/api-client";

export function useBeregnTilsagn() {
  return useMutation<TilsagnBeregningOutput, ApiError, TilsagnBeregningInput>({
    mutationFn: (requestBody: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        requestBody,
      }),
  });
}
