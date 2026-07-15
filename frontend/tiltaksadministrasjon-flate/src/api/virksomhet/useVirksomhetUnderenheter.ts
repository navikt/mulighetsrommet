import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "@tiltaksadministrasjon/api-client";

export function useVirksomhetUnderenheter(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.virksomhetUnderenheter(orgnr),
    queryFn: () => {
      return VirksomhetService.getUnderenheter({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
