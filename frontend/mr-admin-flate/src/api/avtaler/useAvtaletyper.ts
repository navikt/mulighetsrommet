import { useApiQuery } from "@mr/frontend-common";
import { AvtaletypeService, Tiltakskode } from "@tiltaksadministrasjon/api-client";

export function useAvtaletyper(tiltakstype: Tiltakskode) {
  return useApiQuery({
    queryFn: () => AvtaletypeService.getAvtaletyper({ query: { tiltakstype } }),
    queryKey: ["avtaletyper", tiltakstype],
  });
}
