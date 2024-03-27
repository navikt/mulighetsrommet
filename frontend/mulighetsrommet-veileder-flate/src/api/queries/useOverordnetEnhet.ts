import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../client";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useQuery({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () => mulighetsrommetClient.navEnheter.getOverordnetEnhet({ enhetsnummer }),
  });
}
