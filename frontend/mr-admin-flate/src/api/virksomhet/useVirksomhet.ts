import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useVirksomhet(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhet(orgnr),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getVirksomhetByOrgnr({ orgnr });
    },
    enabled: !!orgnr,
  });
}

export function useVirksomhetById(id: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhet(id),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getVirksomhetById({ id });
    },
  });
}
