import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useRegioner() {
  return useSuspenseQuery({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return mulighetsrommetClient.navEnheter.getRegioner();
    },
  });
}
