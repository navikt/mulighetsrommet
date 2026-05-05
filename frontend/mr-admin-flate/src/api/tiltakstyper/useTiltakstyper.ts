import { QueryKeys } from "@/api/QueryKeys";
import {
  SortDirection,
  TiltakstypeEgenskap,
  TiltakstypeService,
  TiltakstypeSortField,
} from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

interface TiltakstypeFilter {
  sort: {
    field: TiltakstypeSortField;
    direction: SortDirection;
  };
  egenskaper?: TiltakstypeEgenskap[];
}

export function useTiltakstyper(filter?: TiltakstypeFilter) {
  const query = filter
    ? {
        sortField: filter.sort.field,
        sortDirection: filter.sort.direction,
        egenskaper: filter.egenskaper,
      }
    : {};

  const { data } = useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstyper(query),
    queryFn: () => TiltakstypeService.getTiltakstyper({ query }),
  });

  return data;
}
