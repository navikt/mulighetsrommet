import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useBrregVirksomhetUnderenheter(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getBrregVirksomhetUnderenheter({ orgnr });
    },
    enabled: !!orgnr,
  });
}
