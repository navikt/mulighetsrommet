import { TilsagnRequest, TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilsagnDefaults(request: TilsagnRequest) {
  return useApiSuspenseQuery({
    queryKey: [QueryKeys.opprettTilsagn(), request],
    queryFn: async () => {
      return TilsagnService.getTilsagnDefaults({
        body: request,
      });
    },
  });
}
