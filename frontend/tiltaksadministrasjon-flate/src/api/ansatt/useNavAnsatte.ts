import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AnsattService, Rolle } from "@tiltaksadministrasjon/api-client";

export function useNavAnsatte(roller: Rolle[]) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navansatte(roller),
    queryFn: () =>
      AnsattService.getAnsatte({
        query: { roller: roller },
      }),
  });
}
