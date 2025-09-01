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

export function useTilsagnTableData(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getAllTilsagn(gjennomforingId),
    queryFn: async () => TilsagnService.getTilsagnTableData({ query: { gjennomforingId } }),
  });
}

export function useAktiveTilsagnTableData(gjennomforingId: string) {
  const statuser = [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT];
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getAllTilsagn(gjennomforingId, statuser),
    queryFn: async () =>
      TilsagnService.getTilsagnTableData({ query: { gjennomforingId, statuser } }),
  });
}
