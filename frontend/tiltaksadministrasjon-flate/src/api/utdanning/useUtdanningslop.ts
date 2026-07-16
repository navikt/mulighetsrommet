import { UtdanningService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useUtdanningslop() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: UtdanningService.getUtdanningslop,
  });
}
