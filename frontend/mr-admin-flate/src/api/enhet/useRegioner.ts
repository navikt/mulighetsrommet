import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useRegioner() {
  return useQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getRegioner();
    },
  });
}
