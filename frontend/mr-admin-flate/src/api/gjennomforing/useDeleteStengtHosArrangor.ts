import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client-v2";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useDeleteStengtHosArrangor(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: async (periodeId: number) => {
      return GjennomforingerService.deleteStengtHosArrangor({
        path: { id: gjennomforingId, periodeId },
      });
    },

    onSuccess() {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(gjennomforingId),
      });
    },
  });
}
