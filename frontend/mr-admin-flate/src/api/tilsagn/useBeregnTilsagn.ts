import {
  BeregnTilsagnService,
  ProblemDetail,
  TilsagnBeregningInput,
  TilsagnBeregningOutput,
} from "@mr/api-client-v2";
import { useMutation } from "@tanstack/react-query";

export function useBeregnTilsagn() {
  return useMutation<{ data: TilsagnBeregningOutput }, ProblemDetail, TilsagnBeregningInput>({
    mutationFn: (body: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        body,
      }),
  });
}
