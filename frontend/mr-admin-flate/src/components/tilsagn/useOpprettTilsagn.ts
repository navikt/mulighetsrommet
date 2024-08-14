import { useMutation } from "@tanstack/react-query";
import { ApiError, TilsagnDto, TilsagnRequest } from "@mr/api-client";
import { TilsagnService } from "@mr/api-client";
import { QueryKeys } from "../../api/QueryKeys";

export function useOpprettTilsagn() {
  return useMutation<TilsagnDto, ApiError, TilsagnRequest>({
    mutationFn: (data) => TilsagnService.opprettTilsagn({ requestBody: data }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}
