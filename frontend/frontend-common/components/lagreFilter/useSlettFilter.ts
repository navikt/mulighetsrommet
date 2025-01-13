import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../../QueryKeys";

export function useSlettFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<{ data: string }, any, string>({
    mutationFn: async (id) => LagretFilterService.slettLagretFilter<true>({ path: { id } }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
