import { QueryKeys } from "@/api/query-keys";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";
import { VeilederService } from "@mr/api-client-v2";

export function useHentVeilederdata() {
  return useSuspenseQueryWrapper({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
