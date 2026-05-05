import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { EndringshistorikkService, EndringshistorikkType } from "@tiltaksadministrasjon/api-client";

export function useEndringshistorikk(id: string, type: EndringshistorikkType) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.historikk(id, type),
    queryFn() {
      return EndringshistorikkService.getEndringshistorikk({
        path: { id },
        query: { type },
      });
    },
  });
}
