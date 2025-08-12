import { TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilOppgjor() {
  return useApiMutation({
    mutationFn: ({
      id,
      aarsaker,
      forklaring,
    }: {
      id: string;
      aarsaker: string[];
      forklaring: string | null;
    }) =>
      TilsagnService.gjorOpp({
        path: { id },
        body: {
          aarsaker,
          forklaring,
        },
      }),
    mutationKey: QueryKeys.gjorOppTilsagn(),
  });
}
