import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useBrregVirksomhetUnderenheter(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getBrregVirksomhetUnderenheter({ orgnr });
    },
    enabled: !!orgnr,
  });
}
