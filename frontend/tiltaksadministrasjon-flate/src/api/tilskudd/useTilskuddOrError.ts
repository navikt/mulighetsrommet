import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TilskuddService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";

export function useTilskudd(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforing(id),
    queryFn: () => TilskuddService.getTilskudd({ path: { tilskuddId: id } }),
  });
}
