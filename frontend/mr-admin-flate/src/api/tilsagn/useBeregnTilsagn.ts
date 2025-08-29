import { BeregnTilsagnRequest, BeregnTilsagnService } from "@mr/api-client-v2";
import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useBeregnTilsagn(request: BeregnTilsagnRequest) {
  const debouncedSerialized = useDebounce(JSON.stringify(request), 300);

  return useApiQuery({
    queryKey: QueryKeys.beregnTilsagn(debouncedSerialized),
    queryFn: () => BeregnTilsagnService.beregnTilsagn({ body: JSON.parse(debouncedSerialized) }),
  });
}
