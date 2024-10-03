import { useMutation } from "@tanstack/react-query";
import { AFTBeregningInput, ApiError, BeregnAftTilsagnService } from "@mr/api-client";

export function useBeregnAFTTilsagn() {
  return useMutation<number, ApiError, AFTBeregningInput>({
    mutationFn: (requestBody: AFTBeregningInput) =>
      BeregnAftTilsagnService.beregnAftTilsagn({
        requestBody,
      }),
  });
}
