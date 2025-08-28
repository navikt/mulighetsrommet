import { TilsagnService, TilsagnStatus } from "@mr/api-client-v2";

import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilsagn(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getTilsagn(id),
    queryFn: async () => {
      return TilsagnService.getTilsagn({ path: { id } });
    },
  });
}

export function useTilsagnEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: ["tilsagn", id, "historikk"],
    queryFn: async () => TilsagnService.getTilsagnEndringshistorikk({ path: { id } }),
  });
}

export function useAktiveTilsagn(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getTilsagnForGjennomforing(id),
    queryFn: async () =>
      TilsagnService.getAll({
        query: {
          gjennomforingId: id,
          statuser: [
            TilsagnStatus.GODKJENT,
            TilsagnStatus.TIL_GODKJENNING,
            TilsagnStatus.RETURNERT,
          ],
        },
      }),
  });
}
