import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretFilter, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

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
