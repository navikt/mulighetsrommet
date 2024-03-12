import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export const useVirksomhetKontaktpersoner = (id?: string) => {
  return useQuery({
    queryKey: QueryKeys.virksomhetKontaktpersoner(id ?? ""),

    queryFn: () =>
      mulighetsrommetClient.virksomhet.hentVirksomhetKontaktpersoner({
        id: id!,
      }),

    enabled: !!id,
  });
};
