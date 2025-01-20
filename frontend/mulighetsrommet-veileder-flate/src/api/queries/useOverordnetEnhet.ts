import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client-v2";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useApiQuery({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () =>
      NavEnheterService.getOverordnetEnhet({
        path: { enhetsnummer },
      }),
  });
}
