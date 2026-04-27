import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { defaultTiltakstypeFilter, TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";

export function useTiltakstyper(filter: TiltakstypeFilterType = defaultTiltakstypeFilter) {
  const queryFilter = {
    query: {
      sortField: filter.sort.field,
      sortDirection: filter.sort.direction,
    },
  };

  const { data } = useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstypeService.getTiltakstyper(queryFilter),
  });

  return data;
}
