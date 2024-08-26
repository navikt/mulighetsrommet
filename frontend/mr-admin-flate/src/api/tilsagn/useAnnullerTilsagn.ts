import { useMutation } from "@tanstack/react-query";
import { ApiError, TilsagnService } from "@mr/api-client";
import { QueryKeys } from "../../api/QueryKeys";

export function useAnnullerTilsagn() {
  return useMutation<unknown, ApiError, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.deleteTilsagn({ id }),
    mutationKey: QueryKeys.annullerTilsagn(),
  });
}
