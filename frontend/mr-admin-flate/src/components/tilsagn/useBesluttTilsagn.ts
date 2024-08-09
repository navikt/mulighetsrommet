import { useMutation } from "@tanstack/react-query";
import { ApiError, BesluttTilsagnRequest, TilsagnService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../api/QueryKeys";

export function useBesluttTilsagn() {
  return useMutation<string, ApiError, { id: string; requestBody: BesluttTilsagnRequest }>({
    mutationFn: ({ id, requestBody }) => TilsagnService.besluttTilsagn({ id, requestBody }),
    mutationKey: QueryKeys.besluttTilsagn(),
  });
}
