import { TilsagnService, TilsagnStatusAarsak } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilAnnullering() {
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
      TilsagnService.tilAnnullering({
        path: { id },
        body: {
          aarsaker,
          forklaring,
        },
      }),
    mutationKey: QueryKeys.annullerTilsagn(),
  });
}
