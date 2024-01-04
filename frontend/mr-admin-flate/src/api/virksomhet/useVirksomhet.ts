import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useVirksomhet(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhetOppslag(orgnr),
    queryFn: async () => {
      const response = await mulighetsrommetClient.virksomhet.hentVirksomhet({ orgnr });
      if (!response) {
        // Fallback for organisasjoner som ikke finnes i Brreg, men som må vi støtte i løsningen
        return { organisasjonsnummer: orgnr, navn: orgnr };
      }
      return response;
    },
    enabled: !!orgnr,
  });
}
