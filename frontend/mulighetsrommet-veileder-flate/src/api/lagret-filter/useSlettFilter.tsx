import { LagretFilterService } from "@mr/api-client-v2";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";

export function useSlettFilter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => LagretFilterService.slettLagretFilter({ path: { id } }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(),
      });
    },
  });
}
