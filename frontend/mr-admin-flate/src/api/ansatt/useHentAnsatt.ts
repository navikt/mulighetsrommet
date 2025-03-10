import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useHentAnsatt() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => AnsattService.hentInfoOmAnsatt(),
  });
}
