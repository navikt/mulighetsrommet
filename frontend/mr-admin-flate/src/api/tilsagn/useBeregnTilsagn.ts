import { useApiMutation } from "@/hooks/useApiMutation";
import {
  BeregnTilsagnService,
  ProblemDetail,
  TilsagnBeregningDto,
  TilsagnBeregningInput,
} from "@mr/api-client-v2";

export function useBeregnTilsagn() {
  return useApiMutation<{ data: TilsagnBeregningDto }, ProblemDetail, TilsagnBeregningInput>({
    mutationFn: (body: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        body,
      }),
  });
}
