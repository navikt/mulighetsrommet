import { useApiSuspenseQuery } from "@mr/frontend-common";
import { KodeverkService, PrismodellInfo, Tiltakskode } from "@tiltaksadministrasjon/api-client";

export function usePrismodeller(tiltakskode: Tiltakskode) {
  const { data: prismodeller } = useApiSuspenseQuery<
    Record<string, PrismodellInfo[]>,
    PrismodellInfo[]
  >({
    queryFn: () => KodeverkService.getPrismodeller(),
    queryKey: ["kodeverk", "prismodeller"],
    select: (data) => data[tiltakskode] ?? [],
  });
  return prismodeller;
}
