import { TilsagnDeltakereRequest, TilsagnService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export function useTilsagnValgbareDeltakere(request: TilsagnDeltakereRequest) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tilsagnValgbareDeltakere(JSON.stringify(request)),
    queryFn: () => TilsagnService.getTilsagnValgbareDeltakere({ body: request }),
  });
}
