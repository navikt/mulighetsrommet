import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@mr/api-client";

export function useKoblingerTilDokumenterForKontaktpersonHosArrangor(kontaktpersonId: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorKontaktpersonKoblinger(kontaktpersonId),
    queryFn: () => ArrangorService.getKoblingerTilDokumenter({ id: kontaktpersonId }),
  });
}
