import { TilsagnService, TilsagnStatusAarsak } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilOppgjor() {
  return useApiMutation({
    mutationFn: ({
      id,
      aarsaker,
      forklaring,
    }: {
      id: string;
      aarsaker: TilsagnStatusAarsak[];
      forklaring: string | null;
    }) =>
      TilsagnService.gjorOpp({
        path: { id },
        body: {
          aarsaker,
          forklaring,
        },
      }),
    mutationKey: QueryKeys.gjorOppTilsagn(),
  });
}
