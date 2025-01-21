import { UtdanningerService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "../QueryKeys";

export function useUtdanningsprogrammer() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: UtdanningerService.getUtdanningsprogrammer,
  });
}
