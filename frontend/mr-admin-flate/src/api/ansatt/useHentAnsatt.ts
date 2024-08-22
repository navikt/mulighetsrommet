import { useQuery } from "@tanstack/react-query";
import { AnsattService, NavAnsatt } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useHentAnsatt() {
  return useQuery<NavAnsatt, Error>({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => AnsattService.hentInfoOmAnsatt(),
  });
}
