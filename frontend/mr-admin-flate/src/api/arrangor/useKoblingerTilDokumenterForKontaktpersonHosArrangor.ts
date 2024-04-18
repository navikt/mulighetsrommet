import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../client";

export function useKoblingerTilDokumenterForKontaktpersonHosArrangor(kontaktpersonId: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorKontaktpersonKoblinger(kontaktpersonId),
    queryFn: () =>
      mulighetsrommetClient.arrangor.getKoblingerTilDokumenter({ id: kontaktpersonId }),
  });
}
