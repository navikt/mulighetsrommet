import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useSyncBrregVirksomhet(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhet(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.syncBrregVirksomhet({ orgnr });
    },
    enabled: !!orgnr,
  });
}

export function useBrregVirksomhetUnderenheter(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhetUnderenheter(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getBrregVirksomhetUnderenheter({ orgnr });
    },
    enabled: !!orgnr,
  });
}
