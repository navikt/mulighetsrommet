import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";

export function useDeltakerRegistreringInnholdselementer() {
  return useApiSuspenseQuery({
    queryKey: ["deltaker-registrering-innholdselementer"],
    queryFn: () => TiltakstypeService.getInnholdselementer(),
    staleTime: Infinity,
  });
}
