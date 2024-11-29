import { useMutation } from "@tanstack/react-query";
import { ApiError, TilsagnService } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useSlettTilsagn() {
  return useMutation<unknown, ApiError, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.slettTilsagn({ id }),
    mutationKey: QueryKeys.slettTilsagn(),
  });
}
