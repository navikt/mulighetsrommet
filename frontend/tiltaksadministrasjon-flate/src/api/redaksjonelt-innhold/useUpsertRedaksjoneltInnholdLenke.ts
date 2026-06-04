import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  ProblemDetail,
  RedaksjoneltInnholdLenke,
  RedaksjoneltInnholdLenkeRequest,
  RedaksjoneltInnholdService,
} from "@tiltaksadministrasjon/api-client";

export function useUpsertRedaksjoneltInnholdLenke(id: string) {
  const queryClient = useQueryClient();

  return useApiMutation<
    { data: RedaksjoneltInnholdLenke },
    ProblemDetail,
    RedaksjoneltInnholdLenkeRequest
  >({
    mutationFn(body: RedaksjoneltInnholdLenkeRequest) {
      return RedaksjoneltInnholdService.upsertLenke({ path: { id }, body });
    },
    onSuccess() {
      return queryClient.invalidateQueries({ queryKey: QueryKeys.redaksjoneltInnholdLenker() });
    },
  });
}
