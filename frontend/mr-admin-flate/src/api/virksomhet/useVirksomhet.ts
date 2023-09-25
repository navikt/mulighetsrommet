import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useVirksomhet(orgnr: string) {
  return useQuery(
    QueryKeys.virksomhetOppslag(orgnr),
    () => mulighetsrommetClient.virksomhet.hentVirksomhet({ orgnr }),
    { enabled: !!orgnr },
  );
}
