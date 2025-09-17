import { BeregnTilsagnRequest, TilsagnService } from "@tiltaksadministrasjon/api-client";
import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useBeregnTilsagn(request: BeregnTilsagnRequest) {
  const debouncedSerialized = useDebounce(JSON.stringify(request), 300);

  return useApiQuery({
    queryKey: QueryKeys.beregnTilsagn(debouncedSerialized),
    queryFn: () => TilsagnService.beregnTilsagn({ body: JSON.parse(debouncedSerialized) }),
    placeholderData: (prev) => prev,
  });
}
