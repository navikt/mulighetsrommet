import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";

export function useSlettFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => LagretFilterService.slettLagretFilter({ path: { id } }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
