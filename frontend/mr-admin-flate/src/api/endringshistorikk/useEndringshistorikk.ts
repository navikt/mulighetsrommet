import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { EndringshistorikkService, DocumentClass } from "@tiltaksadministrasjon/api-client";

export function useEndringshistorikk(id: string, documentClass: DocumentClass) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.historikk(id, documentClass),
    queryFn() {
      return EndringshistorikkService.getEndringshistorikk({
        path: { id },
        query: { documentClass },
      });
    },
  });
}
