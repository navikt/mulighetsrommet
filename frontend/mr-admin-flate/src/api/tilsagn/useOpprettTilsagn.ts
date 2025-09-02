import { TilsagnRequest, TilsagnService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useOpprettTilsagn() {
  return useApiMutation({
    mutationFn: (body: TilsagnRequest) => TilsagnService.opprettTilsagn({ body }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}
