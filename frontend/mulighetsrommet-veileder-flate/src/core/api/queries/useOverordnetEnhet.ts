import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";

export function useOverordnetEnhet(enhetsnummer: string) {
  return useQuery({
    queryKey: QueryKeys.overordnetEnhet(enhetsnummer),
    queryFn: () => mulighetsrommetClient.navEnheter.getOverordnetEnhet({ enhetsnummer }),
  });
}
