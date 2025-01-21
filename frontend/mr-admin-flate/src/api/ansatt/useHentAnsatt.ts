import { useApiQuery } from "@/hooks/useApiQuery";
import { AnsattService, NavAnsatt } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";

export function useHentAnsatt() {
  return useApiQuery<NavAnsatt, Error>({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => AnsattService.hentInfoOmAnsatt(),
  });
}
