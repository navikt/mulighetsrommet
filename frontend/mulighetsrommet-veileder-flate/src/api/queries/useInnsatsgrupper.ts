import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client-v2";

export function useInnsatsgrupper() {
  return useSuspenseQueryWrapper({
    queryKey: QueryKeys.arbeidsmarkedstiltak.innsatsgrupper,
    queryFn: () => VeilederTiltakService.getInnsatsgrupper(),
  });
}
