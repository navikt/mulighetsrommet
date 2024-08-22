import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useQuery({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () =>
      NavEnheterService.getOverordnetEnhet({
        enhetsnummer,
      }),
  });
}
