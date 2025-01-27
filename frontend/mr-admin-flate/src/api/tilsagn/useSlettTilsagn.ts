import { useMutation } from "@tanstack/react-query";
import { TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useSlettTilsagn() {
  return useMutation({
    mutationFn: ({ id }: { id: string }) => TilsagnService.slettTilsagn({ path: { id } }),
    mutationKey: QueryKeys.slettTilsagn(),
  });
}
