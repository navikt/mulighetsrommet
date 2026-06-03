import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useDeleteStengtHosArrangor(gjennomforingId: string) {
  const queryClient = useQueryClient();

  return useApiMutation({
    mutationFn: async (periodeId: number) => {
      return GjennomforingService.deleteStengtHosArrangor({
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
