import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useHentAnsatt() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => AnsattService.me(),
  });
}
