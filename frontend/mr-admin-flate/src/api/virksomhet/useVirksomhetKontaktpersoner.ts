import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export const useVirksomhetKontaktpersoner = (orgnr: string) => {
  return useQuery(QueryKeys.virksomhetKontaktpersoner(orgnr), () =>
    mulighetsrommetClient.virksomhetKontaktperson.hentVirksomhetKontaktpersoner({ orgnr })
  );
}