import {
  TilskuddBehandlingDto,
  TilskuddBehandlingService,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiQuery, useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilskuddBehandling(behandlingId: string) {
  return useApiSuspenseQuery<TilskuddBehandlingDto>({
    queryKey: QueryKeys.tilskuddBehandling(behandlingId),
    queryFn: async () =>
      TilskuddBehandlingService.getTilskuddBehandling({
        path: { tilskuddBehandlingId: behandlingId },
      }),
  });
}

export function usePotentialTilskuddBehandling(behandlingId: string | null) {
  return useApiQuery({
    queryKey: QueryKeys.tilskuddBehandling(behandlingId ?? ""),
    queryFn: async () =>
      TilskuddBehandlingService.getTilskuddBehandling({
        path: { tilskuddBehandlingId: behandlingId ?? "" },
      }),
    enabled: !!behandlingId,
  });
}
