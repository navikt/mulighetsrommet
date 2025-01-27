import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretDokumenttype, LagretFilterRequest, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../query-keys";

export function useLagreFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<any, any, LagretFilterRequest>({
    mutationFn: (body) => LagretFilterService.upsertFilter({ body }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
