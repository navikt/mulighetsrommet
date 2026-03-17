import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { OpplaeringtilskuddService } from "@tiltaksadministrasjon/api-client";

export function useOpplaeringtilskudd() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.opplaeringtilskudd(),
    queryFn: async () => {
      return OpplaeringtilskuddService.getAll();
    },
  });
}
