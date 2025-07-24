import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@api-client";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useApiQuery({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () =>
      NavEnheterService.getOverordnetEnhet({
        path: { enhetsnummer },
      }),
  });
}
