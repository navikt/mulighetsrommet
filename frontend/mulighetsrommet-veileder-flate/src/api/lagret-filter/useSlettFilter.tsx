import { LagretDokumenttype, LagretFilterService } from "@mr/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";

export function useSlettFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useMutation<string, any, string>({
    mutationFn: async (id) => LagretFilterService.slettLagretFilter({ id }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
