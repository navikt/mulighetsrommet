import { useMutation } from "@tanstack/react-query";
import { BeregnTilsagnService, TilsagnBeregningInput } from "@mr/api-client-v2";

export function useBeregnTilsagn() {
  return useMutation({
    mutationFn: (body: TilsagnBeregningInput) =>
      BeregnTilsagnService.beregnTilsagn({
        body,
      }),
  });
}
