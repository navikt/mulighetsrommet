import { TiltakDokumentService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export function useTiltakDokument(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakDokument(id),
    queryFn: () => TiltakDokumentService.getTiltakDokument({ path: { id } }),
  });
}
