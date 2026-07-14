import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { IndividuellGjennomforing } from "./useIndividuelleGjennomforinger";

export function useIndividuellGjennomforing(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.individuellGjennomforing(id),
    queryFn: async (): Promise<{ data: IndividuellGjennomforing }> => {
      const result = await client.get<IndividuellGjennomforing>({
        url: "/api/tiltaksadministrasjon/individuelle-gjennomforinger/{id}",
        path: { id },
      });
      if (!result.data) throw new Error(`Fant ikke individuell gjennomføring med id=${id}`);
      return { data: result.data as IndividuellGjennomforing };
    },
  });
}
