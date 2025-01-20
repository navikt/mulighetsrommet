import { useApiSuspenseQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client-v2";

export function useInnsatsgrupper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.innsatsgrupper,
    queryFn: () => VeilederTiltakService.getInnsatsgrupper(),
  });
}
