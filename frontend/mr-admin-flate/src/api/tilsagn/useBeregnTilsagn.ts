import { useApiMutation } from "@/hooks/useApiMutation";
import {
  BeregnTilsagnService,
  ProblemDetail,
  TilsagnBeregningInput,
  TilsagnBeregningOutput,
} from "@mr/api-client-v2";

export function useBeregnTilsagn() {
  return useApiMutation<{ data: TilsagnBeregningOutput }, ProblemDetail, TilsagnBeregningInput>({
    mutationFn: (body: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        body,
      }),
  });
}
