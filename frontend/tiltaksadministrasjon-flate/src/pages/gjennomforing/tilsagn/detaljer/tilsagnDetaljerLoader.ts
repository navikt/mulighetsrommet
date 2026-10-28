import { TilsagnService, TilsagnStatus } from "@tiltaksadministrasjon/api-client";
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

export function useTilsagnRequest(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tilsagnRequest(id),
    queryFn: async () => {
      return TilsagnService.getTilsagnRequest({ path: { id } });
    },
  });
}

export function useTilsagnTableData(
  gjennomforingId: string,
  filter: {
    page?: number;
    pageSize?: number;
    statuser?: TilsagnStatus[];
    kostnadssteder?: string[];
  },
) {
  const request = {
    query: {
      gjennomforingId,
      statuser: filter.statuser ?? [],
      kostnadssteder: filter.kostnadssteder ?? [],
      page: filter.page ?? 1,
      size: filter.pageSize,
    },
  };
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getAllTilsagn(request),
    queryFn: async () => TilsagnService.getTilsagnTableData(request),
  });
}

export function useAktiveTilsagnTableData(gjennomforingId: string) {
  const statuser = [TilsagnStatus.GODKJENT, TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT];
  return useApiSuspenseQuery({
    queryKey: QueryKeys.getAllTilsagn({ query: { gjennomforingId, statuser } }),
    queryFn: async () =>
      TilsagnService.getTilsagnTableData({ query: { gjennomforingId, statuser } }),
  });
}
