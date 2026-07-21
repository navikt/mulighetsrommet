import { client } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakDokument } from "./useTiltakDokumenter";

export function useTiltakDokument(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakDokument(id),
    queryFn: async (): Promise<{ data: TiltakDokument }> => {
      const result = await client.get<TiltakDokument>({
        url: "/api/tiltaksadministrasjon/tiltak-dokumenter/{id}",
        path: { id },
      });
      if (!result.data) throw new Error(`Fant ikke tiltaksdokument med id=${id}`);
      return { data: result.data as TiltakDokument };
    },
  });
}
