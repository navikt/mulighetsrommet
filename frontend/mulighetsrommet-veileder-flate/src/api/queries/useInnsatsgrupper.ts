import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@api-client";

export function useInnsatsgrupper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.innsatsgrupper,
    queryFn: () => VeilederTiltakService.getInnsatsgrupper(),
  });
}
