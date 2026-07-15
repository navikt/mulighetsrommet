import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export type IndividuellGjennomforingHandling = "PUBLISER" | "REDIGER" | "FORHANDSVIS_I_MODIA";

export function useIndividuellGjennomforingHandlinger(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.individuellGjennomforingHandlinger(id),
    queryFn: async (): Promise<{ data: IndividuellGjennomforingHandling[] }> => {
      const result = await client.get<IndividuellGjennomforingHandling[]>({
        url: "/api/tiltaksadministrasjon/individuelle-gjennomforinger/{id}/handlinger",
        path: { id },
      });
      return { data: (result.data ?? []) as IndividuellGjennomforingHandling[] };
    },
  });
}
