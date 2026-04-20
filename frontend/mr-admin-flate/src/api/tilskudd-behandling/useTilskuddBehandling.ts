import {
  TilskuddBehandlingDto,
  TilskuddBehandlingService,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilskuddBehandling(behandlingId: string) {
  return useApiSuspenseQuery<TilskuddBehandlingDto>({
    queryKey: QueryKeys.tilskuddBehandling(behandlingId),
    queryFn: async () =>
      TilskuddBehandlingService.getTilskuddBehandling({
        path: { tilskuddBehandlingId: behandlingId },
      }),
  });
}
