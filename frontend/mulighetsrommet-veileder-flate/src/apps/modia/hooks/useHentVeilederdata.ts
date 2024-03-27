import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/query-keys";
import { useQuery } from "@tanstack/react-query";

export function useHentVeilederdata() {
  return useQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => mulighetsrommetClient.veileder.getVeileder(),
  });
}
