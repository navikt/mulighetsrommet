import { InnsendingFilterType } from "@/pages/innsendinger/filter";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";

export function useGetInnsendinger(filter: InnsendingFilterType) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.innsendinger({ ...filter }),
    queryFn: () =>
      UtbetalingService.getInnsendinger({
        query: {
          dato: filter.periode.start,
          navEnheter: filter.regioner,
          tiltakstyper: filter.tiltakstyper,
        },
      }),
  });
}
