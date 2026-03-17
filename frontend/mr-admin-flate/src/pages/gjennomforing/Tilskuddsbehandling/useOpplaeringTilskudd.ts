import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { OpplaeringTilskuddService } from "@tiltaksadministrasjon/api-client";

export function useOpplaeringTilskudd() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.opplaeringTilskudd(),
    queryFn: async () => {
      return OpplaeringTilskuddService.getAll();
    },
  });
}
