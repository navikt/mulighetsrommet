import { useMutation } from "@tanstack/react-query";
import { TilsagnRequest } from "@mr/api-client-v2";
import { TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useOpprettTilsagn() {
  return useMutation({
    mutationFn: (body: TilsagnRequest) => TilsagnService.opprettTilsagn({ body }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}
