import { UtdanningerService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "../QueryKeys";

export function useUtdanningsprogrammer() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: UtdanningerService.getUtdanningsprogrammer,
  });
}
