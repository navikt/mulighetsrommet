import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSlettFilter(dokumenttype: LagretDokumenttype) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: async (id: string) => LagretFilterService.slettLagretFilter({ path: { id } }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.lagredeFilter(dokumenttype) }),
      ]);
    },
  });
}
