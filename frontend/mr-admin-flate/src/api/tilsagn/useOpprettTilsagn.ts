import { TilsagnRequest } from "@mr/api-client-v2";
import { TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useOpprettTilsagn() {
  return useApiMutation({
    mutationFn: (body: TilsagnRequest) => TilsagnService.opprettTilsagn({ body }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}
