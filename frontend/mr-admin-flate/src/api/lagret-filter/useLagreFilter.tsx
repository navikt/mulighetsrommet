import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretDokumenttype, LagretFilterRequest, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useLagreFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: LagretFilterRequest) => LagretFilterService.upsertFilter({ body }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });
}
