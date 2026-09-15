import { TilskuddService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTilskuddKompakt(gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tilskuddBehandlinger(gjennomforingId),
    queryFn: async () =>
      TilskuddService.getAllTilskuddKompakt({
        query: { gjennomforingId },
      }),
  });
}
