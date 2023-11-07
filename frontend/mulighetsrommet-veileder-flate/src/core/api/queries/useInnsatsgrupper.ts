import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useInnsatsgrupper() {
  return useQuery({
    queryKey: QueryKeys.sanity.innsatsgrupper,
    queryFn: () => mulighetsrommetClient.sanity.getInnsatsgrupper(),
  });
}
