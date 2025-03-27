import { useMutation } from "@tanstack/react-query";
import { TilsagnService, TilsagnTilAnnulleringAarsak } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useTilsagnTilOppgjor() {
  return useMutation({
    mutationFn: ({
      id,
      aarsaker,
      forklaring,
    }: {
      id: string;
      aarsaker: TilsagnTilAnnulleringAarsak[];
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
