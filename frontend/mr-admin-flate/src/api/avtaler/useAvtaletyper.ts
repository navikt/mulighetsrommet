import { useApiQuery } from "@mr/frontend-common";
import { AvtaletypeInfo, KodeverkService, Tiltakskode } from "@tiltaksadministrasjon/api-client";

export function useAvtaletyper(tiltakskode: Tiltakskode) {
  const { data: avtaletyper } = useApiQuery<
    Record<string, AvtaletypeInfo[]>,
    unknown,
    AvtaletypeInfo[]
  >({
    queryFn: () => KodeverkService.getAvtaletyper(),
    queryKey: ["kodeverk", "avtaletyper"],
    select: (data) => data[tiltakskode] ?? [],
  });
  return avtaletyper ?? [];
}
