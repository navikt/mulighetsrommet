import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretFilter, LagretFilterService } from "@api-client";
import { QueryKeys } from "../query-keys";

export function useLagreFilter() {
  const queryClient = useQueryClient();
  return useMutation<any, any, LagretFilter>({
    mutationFn: (body) => LagretFilterService.upsertFilter({ body }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(),
      });
    },
  });
}
