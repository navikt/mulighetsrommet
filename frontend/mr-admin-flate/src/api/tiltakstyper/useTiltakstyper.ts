import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";

export function useTiltakstyper(filter: TiltakstypeFilterType = {}) {
  const queryFilter = {
    query: {
      sort: filter.sort?.sortString,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstypeService.getTiltakstyper(queryFilter),
  });
}
