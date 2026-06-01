import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { RedaksjoneltInnholdService } from "@tiltaksadministrasjon/api-client";

export function useRedaksjoneltInnholdLenker() {
  const { data } = useApiSuspenseQuery({
    queryKey: QueryKeys.redaksjoneltInnholdLenker(),
    queryFn: () => RedaksjoneltInnholdService.getLenker(),
  });
  return data;
}
