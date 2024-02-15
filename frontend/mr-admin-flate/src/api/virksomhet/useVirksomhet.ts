import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useVirksomhet(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhetOppslag(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.hentVirksomhet({ orgnr });
    },
    enabled: !!orgnr,
  });
}
