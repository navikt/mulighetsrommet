import { useMutation } from "@tanstack/react-query";
import { TilsagnService, TilsagnTilAnnulleringAarsak } from "@mr/api-client-v2";
import { QueryKeys } from "../../api/QueryKeys";

export function useTilsagnTilFrigjoring() {
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
      TilsagnService.tilFrigjoring({
        path: { id },
        body: {
          aarsaker,
          forklaring,
        },
      }),
    mutationKey: QueryKeys.frigjorTilsagn(),
  });
}
