import { TiltakBeskrivelseService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export function useTiltakBeskrivelse(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakBeskrivelse(id),
    queryFn: () => TiltakBeskrivelseService.getTiltakBeskrivelse({ path: { id } }),
  });
}
