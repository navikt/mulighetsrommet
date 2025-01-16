import { useQueryWrapper } from "@/hooks/useQueryWrapper";
import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client-v2";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useQueryWrapper({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () =>
      NavEnheterService.getOverordnetEnhet({
        path: { enhetsnummer },
      }),
  });
}
