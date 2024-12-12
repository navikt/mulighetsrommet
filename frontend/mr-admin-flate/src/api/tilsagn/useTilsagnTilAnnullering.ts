import { useMutation } from "@tanstack/react-query";
import { ApiError, TilsagnService, TilsagnTilAnnulleringAarsak } from "@mr/api-client";
import { QueryKeys } from "../../api/QueryKeys";

export function useTilsagnTilAnnullering() {
  return useMutation<
    unknown,
    ApiError,
    {
      id: string;
      aarsaker: TilsagnTilAnnulleringAarsak[];
      forklaring?: string;
    }
  >({
    mutationFn: ({ id, aarsaker, forklaring }) =>
      TilsagnService.tilAnnullering({
        id,
        requestBody: {
          aarsaker,
          forklaring,
        },
      }),
    mutationKey: QueryKeys.annullerTilsagn(),
  });
}
