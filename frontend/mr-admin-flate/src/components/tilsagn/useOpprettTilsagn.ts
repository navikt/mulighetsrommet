import { useMutation } from "@tanstack/react-query";
import { ApiError, TilsagnDto, TilsagnRequest } from "mulighetsrommet-api-client";
import { TilsagnService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../api/QueryKeys";

export function useOpprettTilsagn() {
  return useMutation<TilsagnDto, ApiError, TilsagnRequest>({
    mutationFn: (data) => TilsagnService.opprettTilsagn({ requestBody: data }),
    mutationKey: QueryKeys.opprettTilsagn(),
  });
}
