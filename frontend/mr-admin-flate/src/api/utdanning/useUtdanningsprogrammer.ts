import { UtdanningService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useUtdanningsprogrammer() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: UtdanningService.getUtdanningsprogrammer,
  });
}
