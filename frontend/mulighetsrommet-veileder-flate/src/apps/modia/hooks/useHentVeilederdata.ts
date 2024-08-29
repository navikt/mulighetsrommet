import { QueryKeys } from "@/api/query-keys";
import { useSuspenseQuery } from "@tanstack/react-query";
import { VeilederService } from "@mr/api-client";

export function useHentVeilederdata() {
  return useSuspenseQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
