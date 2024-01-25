import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";

export function useRegioner() {
  return useQuery({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getRegioner();
    },
  });
}
