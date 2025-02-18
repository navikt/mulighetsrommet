import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";

export function useHentAnsatt() {
  return useApiQuery({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => AnsattService.hentInfoOmAnsatt(),
  });
}
