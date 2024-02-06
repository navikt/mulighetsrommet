import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export const useVirksomhetKontaktpersoner = (orgnr?: string) => {
  return useQuery({
    queryKey: QueryKeys.virksomhetKontaktpersoner(orgnr!!),

    queryFn: () =>
      mulighetsrommetClient.virksomhetKontaktperson.hentVirksomhetKontaktpersoner({
        orgnr: orgnr!!,
      }),

    enabled: !!orgnr,
  });
};
