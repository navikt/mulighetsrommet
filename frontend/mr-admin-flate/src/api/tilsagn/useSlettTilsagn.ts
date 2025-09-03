import { TilsagnService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSlettTilsagn() {
  return useApiMutation({
    mutationFn: ({ id }: { id: string }) => TilsagnService.slettTilsagn({ path: { id } }),
    mutationKey: QueryKeys.slettTilsagn(),
  });
}
