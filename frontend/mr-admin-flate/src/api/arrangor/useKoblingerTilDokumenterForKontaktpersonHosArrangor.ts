import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useKoblingerTilDokumenterForKontaktpersonHosArrangor(kontaktpersonId: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorKontaktpersonKoblinger(kontaktpersonId),
    queryFn: () => ArrangorService.getKoblingerTilDokumenter({ path: { id: kontaktpersonId } }),
  });
}
