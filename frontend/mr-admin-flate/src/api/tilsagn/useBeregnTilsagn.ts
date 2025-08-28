import { BeregnTilsagnRequest, BeregnTilsagnService } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useBeregnTilsagn(request?: BeregnTilsagnRequest) {
  return useApiQuery({
    queryKey: QueryKeys.beregnTilsagn(request),
    queryFn: () => BeregnTilsagnService.beregnTilsagn({ body: request }),
    enabled: !!request,
    retry: false,
    throwOnError: false,
  });
}
