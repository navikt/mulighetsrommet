import { useMutation } from "@tanstack/react-query";
import { TilsagnService, TilsagnTilAnnulleringAarsak } from "@mr/api-client-v2";
import { QueryKeys } from "../../api/QueryKeys";

export function useTilsagnTilAnnullering() {
  return useMutation({
    mutationFn: ({
      id,
      aarsaker,
      forklaring,
    }: {
      id: string;
      aarsaker: TilsagnTilAnnulleringAarsak[];
      forklaring: string | undefined;
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
