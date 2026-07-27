import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export type TiltakDokumentHandling = "PUBLISER" | "REDIGER" | "FORHANDSVIS_I_MODIA";

export function useTiltakDokumentHandlinger(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakDokumentHandlinger(id),
    queryFn: async (): Promise<{ data: TiltakDokumentHandling[] }> => {
      const result = await client.get<TiltakDokumentHandling[]>({
        url: "/api/tiltaksadministrasjon/tiltak-dokumenter/{id}/handlinger",
        path: { id },
      });
      return { data: (result.data ?? []) as TiltakDokumentHandling[] };
    },
  });
}
