import { QueryKeys } from "@/api/query-keys";
import { useQuery } from "@tanstack/react-query";
import { VeilederService } from "@mr/api-client";

export function useHentVeilederdata() {
  return useQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
