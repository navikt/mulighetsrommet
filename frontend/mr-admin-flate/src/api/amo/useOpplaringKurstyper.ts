import { QueryKeys } from "@/api/QueryKeys";
import { KodeverkService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useOpplaringKurstyper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.opplaringKurstyper(),
    queryFn: () => KodeverkService.getOpplaringKurstyper(),
  });
}
