import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  BeregnTilsagnService,
  TilsagnBeregning,
  TilsagnBeregningInput,
} from "@mr/api-client";

export function useBeregnTilsagn() {
  return useMutation<TilsagnBeregning, ApiError, TilsagnBeregningInput>({
    mutationFn: (requestBody: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        requestBody,
      }),
  });
}
