import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretDokumenttype, LagretFilterRequest, LagretFilterService } from "@mr/api-client";
import { QueryKeys } from "../query-keys";

export function useLagreFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<any, any, LagretFilterRequest>({
    mutationFn: (requestBody) => LagretFilterService.upsertFilter({ requestBody }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
