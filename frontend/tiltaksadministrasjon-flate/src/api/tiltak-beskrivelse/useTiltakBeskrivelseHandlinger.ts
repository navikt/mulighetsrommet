import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export type TiltakBeskrivelseHandling = "PUBLISER" | "REDIGER" | "FORHANDSVIS_I_MODIA";

export function useTiltakBeskrivelseHandlinger(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakBeskrivelseHandlinger(id),
    queryFn: async (): Promise<{ data: TiltakBeskrivelseHandling[] }> => {
      const result = await client.get<TiltakBeskrivelseHandling[]>({
        url: "/api/tiltaksadministrasjon/tiltak-beskrivelser/{id}/handlinger",
        path: { id },
      });
      return { data: (result.data ?? []) as TiltakBeskrivelseHandling[] };
    },
  });
}
