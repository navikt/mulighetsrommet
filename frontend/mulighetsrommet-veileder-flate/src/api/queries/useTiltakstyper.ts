import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client-v2";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";

export function useTiltakstyper() {
  return useSuspenseQueryWrapper({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakstyper,
    queryFn: () => VeilederTiltakService.getVeilederflateTiltakstyper(),
  });
}
