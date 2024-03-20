import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useRegioner() {
  return useQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getRegioner();
    },
  });
}
