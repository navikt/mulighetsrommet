import { mulighetsrommetClient } from "@/core/api/clients";
import { QueryKeys } from "@/core/api/query-keys";
import { useQuery } from "@tanstack/react-query";

export function useHentVeilederdata() {
  return useQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => mulighetsrommetClient.veileder.getVeileder(),
  });
}
