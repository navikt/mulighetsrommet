import { TilsagnService, TilsagnTilAnnulleringAarsak } from "@mr/api-client-v2";
import { QueryKeys } from "../../api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilAnnullering() {
  return useApiMutation({
    mutationFn: ({
      id,
      aarsaker,
      forklaring,
    }: {
      id: string;
      aarsaker: TilsagnTilAnnulleringAarsak[];
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
