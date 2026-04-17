import { TilskuddBehandlingService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilskuddBehandlinger(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tilskuddBehandlinger(gjennomforingId),
    queryFn: async () =>
      TilskuddBehandlingService.getTilskuddBehandlingerByGjennomforingId({
        path: { gjennomforingId },
      }),
  });
}
