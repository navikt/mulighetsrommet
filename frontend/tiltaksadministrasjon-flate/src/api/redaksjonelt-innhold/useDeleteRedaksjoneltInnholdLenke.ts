import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { ProblemDetail, RedaksjoneltInnholdService } from "@tiltaksadministrasjon/api-client";

export function useDeleteRedaksjoneltInnholdLenke() {
  const queryClient = useQueryClient();

  return useApiMutation<unknown, ProblemDetail, string>({
    mutationFn: (id: string) => RedaksjoneltInnholdService.deleteLenke({ path: { id } }),
    onSuccess() {
      return queryClient.invalidateQueries({ queryKey: QueryKeys.redaksjoneltInnholdLenker() });
    },
  });
}
